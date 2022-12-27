package tech.quantit.northstar.gateway.api.domain.mktdata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.gateway.api.IMarketCenter;
import tech.quantit.northstar.gateway.api.MarketGateway;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;
import tech.quantit.northstar.gateway.api.domain.contract.ContractDefinition;
import tech.quantit.northstar.gateway.api.domain.contract.ContractDefinition.Type;
import tech.quantit.northstar.gateway.api.domain.contract.GatewayContract;
import tech.quantit.northstar.gateway.api.domain.contract.GroupedContract;
import tech.quantit.northstar.gateway.api.domain.contract.IndexContract;
import tech.quantit.northstar.gateway.api.domain.contract.Instrument;
import tech.quantit.northstar.gateway.api.domain.time.IPeriodHelperFactory;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 市场中心
 * 负责作为网关的防腐层，聚合合约管理以及指数TICK合成
 * @author KevinHuangwl
 *
 */
public class MarketCenter implements IMarketCenter, TickDataAware{
	
	private static final int INIT_SIZE = 16384;
	/* id -> 合约 */
	private final ConcurrentMap<Identifier, Contract> contractMap = new ConcurrentHashMap<>(INIT_SIZE);
	/* 成份合约 -> 指数合约 */
	private final ConcurrentMap<Contract, IndexContract> idxContractMap = new ConcurrentHashMap<>(INIT_SIZE);
	
	private final Table<ExchangeEnum, ProductClassEnum, List<ContractDefinition>> contractDefTbl = HashBasedTable.create();
	
	private final Table<String, ContractDefinition, List<Contract>> gatewayDefContractGroups = HashBasedTable.create();
	
	private final FastEventEngine feEngine;
	
	private final Map<String, IPeriodHelperFactory> gatewayPeriodHelperFactoryMap = new HashMap<>();
	
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
	public synchronized void addInstrument(Instrument ins, MarketGateway gateway, IPeriodHelperFactory phFactory) {
		String gatewayId = gateway.getGatewaySetting().getGatewayId();
		gatewayPeriodHelperFactoryMap.computeIfAbsent(gatewayId, key -> phFactory);
		
		if(!contractDefTbl.contains(ins.exchange(), ins.productClass())) {
			contractDefTbl.put(ins.exchange(), ins.productClass(), new ArrayList<>());
		}
		List<ContractDefinition> defList = contractDefTbl.get(ins.exchange(), ins.productClass());
		for(ContractDefinition def : defList) {
			if(def.getSymbolPattern().matcher(ins.identifier().value()).matches()) {
				Contract contract = new GatewayContract(gateway, feEngine, ins.mergeToContractField(def), phFactory);
				contractMap.put(ins.identifier(), contract);
				
				if(!gatewayDefContractGroups.contains(gatewayId, def)) {					
					gatewayDefContractGroups.put(gatewayId, def, new ArrayList<>());
				}
				gatewayDefContractGroups.get(gatewayId, def).add(contract);
			}
		}
	}

	/**
	 * 加载合约组
	 */
	@Override
	public synchronized void loadContractGroup(String gatewayId) {
		if(!gatewayPeriodHelperFactoryMap.containsKey(gatewayId)) {
			throw new IllegalStateException(String.format("[%s] 没有合约注册信息", gatewayId));
		}
		IPeriodHelperFactory factory = gatewayPeriodHelperFactoryMap.get(gatewayId);
		List<Contract> gatewayContracts = getContracts(gatewayId);
		// 聚合期权合约
		aggregateOptionContracts(gatewayContracts.stream().filter(c -> c.productClass() == ProductClassEnum.OPTION).toList());
		
		// 聚合期货合约
		aggregateFutureIndexContracts(gatewayDefContractGroups.row(gatewayId), factory);
		
		// 聚合其他合约
		aggregateGroupedContracts(gatewayDefContractGroups.row(gatewayId));
	}
	
	private void aggregateOptionContracts(List<Contract> optContracts) {
		Map<String, List<Contract>> symbolOptionsMap = new HashMap<>();
		for(Contract c : optContracts) {
			String underlyingSymbol = c.contractField().getUnderlyingSymbol();
			symbolOptionsMap.computeIfAbsent(underlyingSymbol, key -> new ArrayList<>());
			symbolOptionsMap.get(underlyingSymbol).add(c);
		}
		for(Entry<String, List<Contract>> e : symbolOptionsMap.entrySet()) {
			Contract c = new GroupedContract(String.format("%s_期权链", e.getKey()), e.getValue());
			contractMap.put(c.identifier(), c);
		}
	}
	
	private void aggregateFutureIndexContracts(Map<ContractDefinition, List<Contract>> contractDefMap, IPeriodHelperFactory factory) {
		for(Entry<ContractDefinition, List<Contract>> e : contractDefMap.entrySet()) {
			if(e.getKey().getType() != Type.INDEX) {
				continue;
			}
			ContractField idxContractField = makeIndexContractField(e.getValue().get(0).contractField());
			IndexContract c = new IndexContract(feEngine, idxContractField, e.getValue(), factory);
			contractMap.put(c.identifier(), c);
		}
	}
	
	private ContractField makeIndexContractField(ContractField proto) {
		String name = proto.getName().replaceAll("\\d+", "指数");
		String fullName = proto.getFullName().replaceAll("\\d+", "指数");
		String originSymbol = proto.getSymbol();
		String symbol = originSymbol.replaceAll("\\d+", Constants.INDEX_SUFFIX);
		String contractId = proto.getContractId().replace(originSymbol, symbol);
		String thirdPartyId = proto.getThirdPartyId().replace(originSymbol, symbol);
		String unifiedSymbol = proto.getUnifiedSymbol().replace(originSymbol, symbol);
		return ContractField.newBuilder(proto)
				.setSymbol(symbol)
				.setThirdPartyId(thirdPartyId)
				.setContractId(contractId)
				.setLastTradeDateOrContractMonth("")
				.setUnifiedSymbol(unifiedSymbol)
				.setFullName(fullName)
				.setLongMarginRatio(0.1)
				.setShortMarginRatio(0.1)
				.setName(name)
				.build();
	}
	
	private void aggregateGroupedContracts(Map<ContractDefinition, List<Contract>> contractDefMap) {
		for(Entry<ContractDefinition, List<Contract>> e : contractDefMap.entrySet()) {
			if(e.getKey().getType() != Type.OTHERS) {
				continue;
			}
			Contract c = new GroupedContract(e.getKey().getName(), e.getValue());
			contractMap.put(c.identifier(), c);
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
	 * 获取网关合约
	 */
	@Override
	public List<Contract> getContracts(String gatewayId) {
		if(StringUtils.isBlank(gatewayId)) {			
			return contractMap.values().stream().toList();
		}
		return contractMap.values().stream().filter(c -> StringUtils.equals(gatewayId, c.gatewayId())).toList();
	}

	/**
	 * 更新行情
	 */
	@Override
	public void onTick(TickField tick) {
		Identifier id = new Identifier(tick.getUnifiedSymbol()); 
		// 更新普通合约
		Contract contract = getContract(id);
		if(contract instanceof TickDataAware tdAware) {
			tdAware.onTick(tick);
		}
		
		// 更新指数合约
		IndexContract idxContract = idxContractMap.get(contract);
		if(Objects.nonNull(idxContract)) {
			idxContract.onTick(tick);
		}
	}

}
