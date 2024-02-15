package org.dromara.northstar.gateway.mktdata;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.Instrument;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.contract.GatewayContract;
import org.dromara.northstar.gateway.contract.IndexContract;
import org.dromara.northstar.gateway.contract.OptionChainContract;
import org.dromara.northstar.gateway.contract.PrimaryContract;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

/**
 * 市场中心
 * 负责作为网关的防腐层，聚合合约管理以及指数TICK合成
 * @author KevinHuangwl
 *
 */
@Slf4j
public class MarketCenter implements IMarketCenter{
	
	private static final int INIT_SIZE = 16384;
	/* id -> 合约 */
	private final ConcurrentMap<Identifier, IContract> contractMap = new ConcurrentHashMap<>(INIT_SIZE);
	/* 成份合约 -> 指数合约 */
	private final ConcurrentMap<IContract, IndexContract> idxContractMap = new ConcurrentHashMap<>(INIT_SIZE);

	private final Table<ChannelType, String, IContract> channelSymbolContractTbl = HashBasedTable.create();

	private final Table<ExchangeEnum, ProductClassEnum, List<ContractDefinition>> contractDefTbl = HashBasedTable.create();
	
	private final Table<ChannelType, ContractDefinition, List<IContract>> channelDefContractGroups = HashBasedTable.create();
	
	private final Map<ChannelType, MarketGateway> gatewayMap = new EnumMap<>(ChannelType.class);
	
	private final ConcurrentMap<Contract, Tick> tickMap = new ConcurrentHashMap<>();
	
	private final FastEventEngine feEngine;
	
	private Set<ChannelType> loadedGroupOfChannel = new HashSet<>();
	
	public MarketCenter(FastEventEngine feEngine) {
		this.feEngine = feEngine;
	}
	
	/**
	 * 增加合约定义
	 */
	@Override
	public void addDefinitions(List<ContractDefinition> contractDefs) {
		for(ContractDefinition def : contractDefs) {
			if(!contractDefTbl.contains(def.exchange(), def.productClass())) {				
				contractDefTbl.put(def.exchange(), def.productClass(), new ArrayList<>(512));
			}
			contractDefTbl.get(def.exchange(), def.productClass()).add(def);
		}
	}
	
	/**
	 * 注册网关合约
	 */
	@Override
	public synchronized void addInstrument(Instrument ins) {
		// 绑定合约定义
		getDefinition(ins.exchange(), ins.productClass(), ins.identifier().value())
			.ifPresent(def -> {
				log.debug("[{}] 匹配合约定义 [{} {} {}]", ins.identifier().value(), def.exchange(), def.productClass(), def.symbolPattern().pattern());
				ins.setContractDefinition(def);
				IContract contract = new GatewayContract(this, feEngine, ins);
				contractMap.put(ins.identifier(), contract);
				
				if(!channelDefContractGroups.contains(ins.channelType(), def)) {					
					channelDefContractGroups.put(ins.channelType(), def, new ArrayList<>());
				}
				channelDefContractGroups.get(ins.channelType(), def).add(contract);
				channelSymbolContractTbl.put(contract.channelType(), contract.contract().symbol(), contract);
				channelSymbolContractTbl.put(contract.channelType(), contract.contract().unifiedSymbol(), contract);
			});

		if(!contractMap.containsKey(ins.identifier())) {
			log.debug("未找到 [{}] 的合约定义，忽略该合约的注册", ins.identifier().value());
		}
	}
	
	@Override
	public Optional<ContractDefinition> getDefinition(ExchangeEnum exchange, ProductClassEnum productClass, String identifier){
		List<ContractDefinition> defList = contractDefTbl.get(exchange, productClass);
		if(Objects.isNull(defList)) {
			return Optional.empty();
		}
		for(ContractDefinition def : defList) {
			if(def.symbolPattern().matcher(identifier).matches()) {
				return Optional.of(def);
			}
		}
		return Optional.empty();
	}

	/**
	 * 加载合约组
	 */
	@Override
	public synchronized void loadContractGroup(ChannelType channelType) {
		List<IContract> gatewayContracts = getContracts(channelType);
		Map<String, IContract> symbolContractMap = new HashMap<>();
		for(IContract c : gatewayContracts) {
			symbolContractMap.put(c.contract().symbol(), c);
		}
		// 聚合期权合约
		try {
			aggregateOptionContracts(gatewayContracts.stream().filter(c -> c.productClass() == ProductClassEnum.OPTION).toList(), symbolContractMap);
		} catch (Exception e) {
			log.error("聚合期权链合约时出错", e);
		}
		
		// 聚合期货合约
		try {
			aggregateFutureIndexContracts(channelDefContractGroups.row(channelType));
		} catch (Exception e) {
			log.error("聚合期货指数合约时出错", e);
		}
		
		loadedGroupOfChannel.add(channelType);
	}
	
	private void aggregateOptionContracts(List<IContract> optContracts, Map<String, IContract> symbolContractMap) {
		Map<String, List<IContract>> symbolOptionsMap = new HashMap<>();
		for(IContract c : optContracts) {
			if(c instanceof OptionChainContract) {
				continue;
			}
			String underlyingSymbol = c.contract().underlyingSymbol();
			symbolOptionsMap.computeIfAbsent(underlyingSymbol, key -> new ArrayList<>());
			symbolOptionsMap.get(underlyingSymbol).add(c);
		}
		for(Entry<String, List<IContract>> e : symbolOptionsMap.entrySet()) {
			if(!symbolContractMap.containsKey(e.getKey())) {
				log.warn("找不到{}对应的合约信息", e.getKey());
				continue;
			}
			IContract c = new OptionChainContract(symbolContractMap.get(e.getKey()), e.getValue());
			contractMap.put(c.identifier(), c);
		}
	}
	
	private void aggregateFutureIndexContracts(Map<ContractDefinition, List<IContract>> contractDefMap) {
		for(Entry<ContractDefinition, List<IContract>> e : contractDefMap.entrySet()) {
			if(e.getKey().productClass() != ProductClassEnum.FUTURES) {
				continue;
			}
			IndexContract c = new IndexContract(feEngine, e.getValue());
			contractMap.put(c.identifier(), c);
			for(IContract memberContract : c.memberContracts()) {
				idxContractMap.put(memberContract, c);
			}
			channelSymbolContractTbl.put(c.channelType(), c.contract().symbol(), c);
			channelSymbolContractTbl.put(c.channelType(), c.contract().unifiedSymbol(), c);
			
			// CTP主连合约生成 
			if(c.channelType() == ChannelType.PLAYBACK) {
				PrimaryContract pc = new PrimaryContract(c);
				contractMap.put(pc.identifier(), pc);
				channelSymbolContractTbl.put(pc.channelType(), pc.contract().symbol(), pc);
				channelSymbolContractTbl.put(pc.channelType(), pc.contract().unifiedSymbol(), pc);
			}
		}
	}
	
	/**
	 * 查找合约
	 */
	@Override
	public IContract getContract(Identifier identifier) {
		if(!contractMap.containsKey(identifier)) {
			throw new NoSuchElementException(String.format("找不到合约：%s", identifier.value()));
		}
		return contractMap.get(identifier);
	}

	/**
	 * 查询合约
	 */
	@Override
	public IContract getContract(ChannelType channelType, String code) {
		if(!channelSymbolContractTbl.contains(channelType, code)) {
			throw new NoSuchElementException(String.format("找不到合约：%s -> %s", channelType, code));
		}
		return channelSymbolContractTbl.get(channelType, code);
	}
	
	/**
	 * 获取网关合约
	 */
	@Override
	public List<IContract> getContracts(String gatewayId) {
		if(StringUtils.isBlank(gatewayId)) {			
			return contractMap.values().stream().toList();
		}
		return contractMap.values()
				.stream()
				.filter(c -> StringUtils.equals(gatewayId, c.gatewayId()))
				.toList();
	}
	
	/**
	 * 获取网关合约
	 */
	@Override
	public List<IContract> getContracts(ChannelType channelType) {
		return contractMap.values()
				.stream()
				.filter(c -> c.channelType() == channelType)
				.toList();
	}

	/**
	 * 更新行情
	 */
	@Override
	public void onTick(Tick tick) {
		// 确保tickMap中仅保留最新数据，可以避免同时接收历史行情与实时行情时的数据混乱
		// 此处需要进行重复过滤处理
		if(tickMap.containsKey(tick.contract()) && tickMap.get(tick.contract()).actionTimestamp() >= tick.actionTimestamp()) {
			return;
		}
		tickMap.put(tick.contract(), tick);
		
		if(tick.contract().unifiedSymbol().contains(Constants.INDEX_SUFFIX)) {
			return; // 直接忽略指数TICK的后续处理
		}
		
		// 更新普通合约
		IContract contract = getContract(tick.channelType(), tick.contract().unifiedSymbol());
		if(contract instanceof TickDataAware tdAware) {
			tdAware.onTick(tick);
		}
		
		// 更新指数合约
		IndexContract idxContract = idxContractMap.get(contract);
		if(Objects.nonNull(idxContract)) {
			idxContract.onTick(tick);
		} else if(contract.productClass() == ProductClassEnum.FUTURES && loadedGroupOfChannel.contains(tick.channelType())){
			log.trace("没有找到 [{}] 对应的指数合约", contract.identifier());
		}
	}

	/**
	 * 增加网关
	 */
	@Override
	public void addGateway(MarketGateway gateway) {
		log.info("注册网关：{}", gateway.gatewayId());
		gatewayMap.put(gateway.channelType(), gateway);
	}

	@Override
	public MarketGateway getGateway(ChannelType channelType) {
		return gatewayMap.get(channelType);
	}

	@Override
	public Optional<Tick> lastTick(Contract contract) {
		return Optional.ofNullable(tickMap.get(contract));
	}

}
