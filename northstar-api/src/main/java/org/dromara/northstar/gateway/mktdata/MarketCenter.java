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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.exception.NoSuchContractException;
import org.dromara.northstar.common.model.ContractConfigDTO;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IContractDefinitionProvider;
import org.dromara.northstar.gateway.IContractManager;
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
/* 注意，本类的日志输出在logs/DEBUG/MarketCenter_*.log文件 */
@Slf4j 
public class MarketCenter implements IMarketCenter, IContractManager{
	
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
	
	private final Map<Identifier, ContractConfigDTO> contractConfigMap;
	
	public MarketCenter(FastEventEngine feEngine, List<IContractDefinitionProvider> contractDefProviders, 
			Map<Identifier, ContractConfigDTO> contractConfigMap) {
		this.feEngine = feEngine;
		this.contractConfigMap = contractConfigMap;
		contractDefProviders.forEach(pvd -> addDefinitions(pvd.get()));
	}
	
	/**
	 * 增加合约定义
	 */
	private void addDefinitions(List<ContractDefinition> contractDefs) {
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
		if(contractMap.containsKey(ins.identifier())) {
			log.debug("[{}] 合约已注册，不需要重复注册", ins.identifier().value());
			return;
		}
		
		// 绑定合约定义
		getDefinition(ins.exchange(), ins.productClass(), ins.identifier().value())
			.ifPresent(def -> {
				log.debug("[{}] 匹配合约定义 [{} {} {}]", ins.identifier().value(), def.exchange(), def.productClass(), def.symbolPattern().pattern());
				ContractConfigDTO config = contractConfigMap.get(ins.identifier());
				if(config != null) {
					// 使用用户自定义合约
					ContractDefinition customDef = ContractDefinition.builder()
							.commissionFee(config.getCommissionFee())
							.commissionRate(config.getCommissionRate())
							.marginRatio(config.getMarginRatio())
							.productClass(def.productClass())
							.dataSource(def.dataSource())
							.exchange(def.exchange())
							.tradeTimeDef(def.tradeTimeDef())
							.name(ins.identifier().toString() + "自定义")
							.build();
					ins.setContractDefinition(customDef);
				} else {
					// 使用默认合约定义
					ins.setContractDefinition(def);
				}
				
				IContract contract = new GatewayContract(feEngine, ins);
				contractMap.put(ins.identifier(), contract);
				
				if(!channelDefContractGroups.contains(ins.channelType(), def)) {					
					channelDefContractGroups.put(ins.channelType(), def, new ArrayList<>());
				}
				channelDefContractGroups.get(ins.channelType(), def).add(contract);
				channelSymbolContractTbl.put(contract.channelType(), contract.contract().symbol(), contract);
				channelSymbolContractTbl.put(contract.channelType(), contract.contract().unifiedSymbol(), contract);
				if(log.isTraceEnabled()) {					
					log.trace("合约登记成功：{}", contract.contract());
				}
			});

		if(!contractMap.containsKey(ins.identifier())) {
			log.info("未找到 [{}] 的合约定义，忽略该合约的注册", ins.identifier().value());
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
			Contract cc = c.contract();
			contractMap.put(c.identifier(), c);
			channelSymbolContractTbl.put(c.channelType(), cc.symbol(), c);
			channelSymbolContractTbl.put(c.channelType(), cc.unifiedSymbol(), c);
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
			throw new NoSuchContractException(identifier.value());
		}
		return contractMap.get(identifier);
	}

	/**
	 * 查询合约
	 */
	@Override
	public IContract getContract(ChannelType channelType, String code) {
		if(!channelSymbolContractTbl.contains(channelType, code)) {
			throw new NoSuchContractException(String.format("%s -> %s", channelType, code));
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
	
	public Map<ChannelType, List<Contract>> channelContracts(){
		return contractMap.values()
				.stream()
				.map(IContract::contract)
				.collect(Collectors.groupingBy(c -> c.channelType()));
	}

	/**
	 * 更新行情
	 */
	@Override
	public void onTick(Tick tick) {
		// 避免同一个TICK进入两次
		if(tickMap.containsKey(tick.contract()) && tickMap.get(tick.contract()) == tick) {
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
		log.info("注册网关：{}", gateway.getDescription().getGatewayId());
		gatewayMap.put(gateway.getDescription().getChannelType(), gateway);
	}

	@Override
	public MarketGateway getGateway(ChannelType channelType) {
		return gatewayMap.get(channelType);
	}

	@Override
	public Optional<Tick> lastTick(Contract contract) {
		return Optional.ofNullable(tickMap.get(contract));
	}

	public List<Tick> latestTicks(){
		return tickMap.values().stream().toList();
	}
}
