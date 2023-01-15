package tech.quantit.northstar.gateway.api.domain.mktdata;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.gateway.api.IMarketCenter;
import tech.quantit.northstar.gateway.api.MarketGateway;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;
import tech.quantit.northstar.gateway.api.domain.contract.ContractDefinition;
import tech.quantit.northstar.gateway.api.domain.contract.GatewayContract;
import tech.quantit.northstar.gateway.api.domain.contract.IndexContract;
import tech.quantit.northstar.gateway.api.domain.contract.Instrument;
import tech.quantit.northstar.gateway.api.domain.contract.OptionChainContract;
import tech.quantit.northstar.gateway.api.domain.contract.PrimaryContract;
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
public class MarketCenter implements IMarketCenter, TickDataAware{
	
	private static final int INIT_SIZE = 16384;
	/* id -> 合约 */
	private final ConcurrentMap<Identifier, Contract> contractMap = new ConcurrentHashMap<>(INIT_SIZE);
	/* 成份合约 -> 指数合约 */
	private final ConcurrentMap<Contract, IndexContract> idxContractMap = new ConcurrentHashMap<>(INIT_SIZE);

	private final Table<String, String, Contract> gatewaySymbolContractTbl = HashBasedTable.create();
	private final Table<String, String, Contract> gatewayUnifiedSymbolContractTbl = HashBasedTable.create();

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
				gatewaySymbolContractTbl.put(contract.gatewayId(), contract.contractField().getSymbol(), contract);
				gatewaySymbolContractTbl.put(contract.gatewayId(), contract.contractField().getUnifiedSymbol(), contract);
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
		// 聚合期权合约
		aggregateOptionContracts(gatewayContracts.stream().filter(c -> c.productClass() == ProductClassEnum.OPTION).toList());
		
		// 聚合期货合约
		aggregateFutureIndexContracts(channelDefContractGroups.row(channelType));
		
	}
	
	private void aggregateOptionContracts(List<Contract> optContracts) {
		Map<String, List<Contract>> symbolOptionsMap = new HashMap<>();
		for(Contract c : optContracts) {
			String underlyingSymbol = c.contractField().getUnderlyingSymbol();
			symbolOptionsMap.computeIfAbsent(underlyingSymbol, key -> new ArrayList<>());
			symbolOptionsMap.get(underlyingSymbol).add(c);
		}
		for(Entry<String, List<Contract>> e : symbolOptionsMap.entrySet()) {
			Contract c = new OptionChainContract(String.format("%s_期权链", e.getKey()), e.getValue());
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
			gatewaySymbolContractTbl.put(c.gatewayId(), c.contractField().getSymbol(), c);
			gatewayUnifiedSymbolContractTbl.put(c.gatewayId(), c.contractField().getUnifiedSymbol(), c);
			
			// CTP主力合约生成 
			if(c.channelType() == ChannelType.CTP) {
				PrimaryContract pc = new PrimaryContract(c);
				contractMap.put(pc.identifier(), pc);
				gatewaySymbolContractTbl.put(pc.gatewayId(), pc.contractField().getSymbol(), pc);
				gatewayUnifiedSymbolContractTbl.put(pc.gatewayId(), pc.contractField().getUnifiedSymbol(), pc);
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
	public Contract getContract(String gatewayId, String code) {
		Contract c1 = gatewaySymbolContractTbl.get(gatewayId, code);
		Contract c2 = gatewayUnifiedSymbolContractTbl.get(gatewayId, code);
		if(Objects.isNull(c1) && Objects.isNull(c2)) {
			throw new NoSuchElementException(String.format("找不到合约：%s -> %s", gatewayId, code));
		}
		return Optional.ofNullable(c1).orElse(c2);
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
		Contract contract = getContract(tick.getGatewayId(), tick.getUnifiedSymbol());
		if(contract instanceof TickDataAware tdAware) {
			tdAware.onTick(tick);
		}
		
		// 更新指数合约
		IndexContract idxContract = idxContractMap.get(contract);
		if(Objects.nonNull(idxContract)) {
			idxContract.onTick(tick);
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
