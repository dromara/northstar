package org.dromara.northstar.gateway.mktdata;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.Instrument;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.contract.GatewayContract;
import org.dromara.northstar.gateway.contract.IndexContract;
import org.dromara.northstar.gateway.contract.OptionChainContract;
import org.dromara.northstar.gateway.contract.PrimaryContract;
import org.dromara.northstar.gateway.model.ContractDefinition;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.TickField;

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
	private final ConcurrentMap<Identifier, Contract> contractMap = new ConcurrentHashMap<>(INIT_SIZE);
	/* 成份合约 -> 指数合约 */
	private final ConcurrentMap<Contract, IndexContract> idxContractMap = new ConcurrentHashMap<>(INIT_SIZE);

	private final Table<ChannelType, String, Contract> channelSymbolContractTbl = HashBasedTable.create();

	private final Table<ExchangeEnum, ProductClassEnum, List<ContractDefinition>> contractDefTbl = HashBasedTable.create();
	
	private final Table<ChannelType, ContractDefinition, List<Contract>> channelDefContractGroups = HashBasedTable.create();
	
	private final Map<ChannelType, MarketGateway> gatewayMap = new EnumMap<>(ChannelType.class);
	
	private final FastEventEngine feEngine;
	
	public MarketCenter(List<ContractDefinition> contractDefs, FastEventEngine feEngine) {
		this.feEngine = feEngine;
		for(ContractDefinition def : contractDefs) {
			if(!contractDefTbl.contains(def.getExchange(), def.getProductClass())) {				
				contractDefTbl.put(def.getExchange(), def.getProductClass(), new ArrayList<>(512));
			}
			contractDefTbl.get(def.getExchange(), def.getProductClass()).add(def);
		}
	}
	
	/**
	 * 注册网关合约
	 */
	@Override
	public synchronized void addInstrument(Instrument ins) {
		// 绑定合约定义
		List<ContractDefinition> defList = contractDefTbl.get(ins.exchange(), ins.productClass());
		if(Objects.isNull(defList)) {
			log.debug("未找到 [{}] 的合约定义，忽略该合约的注册", ins.identifier().value());
			return;
		}
		for(ContractDefinition def : defList) {
			if(def.getSymbolPattern().matcher(ins.identifier().value()).matches()) {
				log.debug("[{}] 匹配合约定义 [{} {} {}]", ins.identifier().value(), def.getExchange(), def.getProductClass(), def.getSymbolPattern().pattern());
				ins.setContractDefinition(def);
				Contract contract = new GatewayContract(this, feEngine, ins);
				contractMap.put(ins.identifier(), contract);
				
				if(!channelDefContractGroups.contains(ins.channelType(), def)) {					
					channelDefContractGroups.put(ins.channelType(), def, new ArrayList<>());
				}
				channelDefContractGroups.get(ins.channelType(), def).add(contract);
				channelSymbolContractTbl.put(contract.channelType(), contract.contractField().getSymbol(), contract);
				channelSymbolContractTbl.put(contract.channelType(), contract.contractField().getUnifiedSymbol(), contract);
			}
		}

		if(!contractMap.containsKey(ins.identifier())) {
			log.debug("未找到 [{}] 的合约定义，忽略该合约的注册", ins.identifier().value());
		}
	}

	/**
	 * 加载合约组
	 */
	@Override
	public synchronized void loadContractGroup(ChannelType channelType) {
		List<Contract> gatewayContracts = getContracts(channelType);
		Map<String, Contract> symbolContractMap = new HashMap<>();
		for(Contract c : gatewayContracts) {
			symbolContractMap.put(c.contractField().getSymbol(), c);
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
		
	}
	
	private void aggregateOptionContracts(List<Contract> optContracts, Map<String,Contract> symbolContractMap) {
		Map<String, List<Contract>> symbolOptionsMap = new HashMap<>();
		for(Contract c : optContracts) {
			if(c instanceof OptionChainContract) {
				continue;
			}
			String underlyingSymbol = c.contractField().getUnderlyingSymbol();
			symbolOptionsMap.computeIfAbsent(underlyingSymbol, key -> new ArrayList<>());
			symbolOptionsMap.get(underlyingSymbol).add(c);
		}
		for(Entry<String, List<Contract>> e : symbolOptionsMap.entrySet()) {
			if(!symbolContractMap.containsKey(e.getKey())) {
				log.warn("找不到{}对应的合约信息", e.getKey());
				continue;
			}
			Contract c = new OptionChainContract(symbolContractMap.get(e.getKey()), e.getValue());
			contractMap.put(c.identifier(), c);
		}
	}
	
	private void aggregateFutureIndexContracts(Map<ContractDefinition, List<Contract>> contractDefMap) {
		for(Entry<ContractDefinition, List<Contract>> e : contractDefMap.entrySet()) {
			if(e.getKey().getProductClass() != ProductClassEnum.FUTURES) {
				continue;
			}
			IndexContract c = new IndexContract(feEngine, e.getValue());
			contractMap.put(c.identifier(), c);
			for(Contract memberContract : c.memberContracts()) {
				idxContractMap.put(memberContract, c);
			}
			channelSymbolContractTbl.put(c.channelType(), c.contractField().getSymbol(), c);
			channelSymbolContractTbl.put(c.channelType(), c.contractField().getUnifiedSymbol(), c);
			
			// CTP主力合约生成 
			if(c.channelType() == ChannelType.CTP) {
				PrimaryContract pc = new PrimaryContract(c);
				contractMap.put(pc.identifier(), pc);
				channelSymbolContractTbl.put(pc.channelType(), pc.contractField().getSymbol(), pc);
				channelSymbolContractTbl.put(pc.channelType(), pc.contractField().getUnifiedSymbol(), pc);
			}
		}
	}
	
	/**
	 * 查找合约
	 */
	@Override
	public Contract getContract(Identifier identifier) {
		if(!contractMap.containsKey(identifier)) {
			throw new NoSuchElementException(String.format("找不到合约：%s", identifier.value()));
		}
		return contractMap.get(identifier);
	}

	/**
	 * 查询合约
	 */
	@Override
	public Contract getContract(ChannelType channelType, String code) {
		if(!channelSymbolContractTbl.contains(channelType, code)) {
			throw new NoSuchElementException(String.format("找不到合约：%s -> %s", channelType, code));
		}
		return channelSymbolContractTbl.get(channelType, code);
	}
	
	/**
	 * 获取网关合约
	 */
	@Override
	public List<Contract> getContracts(String gatewayId) {
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
	public List<Contract> getContracts(ChannelType channelType) {
		return contractMap.values()
				.stream()
				.filter(c -> c.channelType() == channelType)
				.toList();
	}

	/**
	 * 更新行情
	 */
	@Override
	public void onTick(TickField tick) {
		// 更新普通合约
		Contract contract = getContract(ChannelType.valueOf(tick.getChannelType()), tick.getUnifiedSymbol());
		if(contract instanceof TickDataAware tdAware) {
			tdAware.onTick(tick);
		}
		
		// 更新指数合约
		IndexContract idxContract = idxContractMap.get(contract);
		if(Objects.nonNull(idxContract)) {
			idxContract.onTick(tick);
		} else if(contract.productClass() == ProductClassEnum.FUTURES){
			log.trace("没有找到 [{}] 对应的指数合约", contract.identifier());
		}
	}

	@Override
	public void endOfMarketTime() {
		contractMap.values().stream()
			.filter(TickDataAware.class::isInstance)
			.map(TickDataAware.class::cast)
			.forEach(TickDataAware::endOfMarket);
	}

	/**
	 * 增加网关
	 */
	@Override
	public void addGateway(MarketGateway gateway) {
		log.info("注册网关渠道：{}", gateway.gatewayId());
		gatewayMap.put(gateway.channelType(), gateway);
	}

	@Override
	public MarketGateway getGateway(ChannelType channelType) {
		return gatewayMap.get(channelType);
	}

}
