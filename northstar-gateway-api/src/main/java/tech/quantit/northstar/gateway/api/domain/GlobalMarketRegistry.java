package tech.quantit.northstar.gateway.api.domain;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.gateway.api.MarketDataBuffer;
import tech.quantit.northstar.gateway.api.MarketGateway;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 全局市场注册中心
 * 负责合约注册及订阅
 * 负责路由行情数据到合适的BAR生成器或指数生成器
 * @author KevinHuangwl
 *
 */
@Slf4j
public class GlobalMarketRegistry {

	protected FastEventEngine feEngine;
	
	protected Map<GatewayType, SubscriptionManager> subMgrMap = new EnumMap<>(GatewayType.class);
	
	protected Map<GatewayType, MarketGateway> gatewayMap = new EnumMap<>(GatewayType.class);
	
	/**
	 * 合约表
	 * unifiedSymbol --> contract
	 */
	protected Map<String, NormalContract> contractMap = new HashMap<>(4096);
	/**
	 * 指数生成器表
	 * unifiedSymbol --> ticker
	 */
	protected Map<String, IndexTicker> idxTickerMap = new HashMap<>(1024);
	/**
	 * BAR生成器表
	 * unifiedSymbol --> barGen
	 */
	protected Map<String, BarGenerator> barGenMap = new HashMap<>(4096);
	
	protected Consumer<ContractField> onContractSubsciption;

	protected Consumer<NormalContract> onContractSave;
	
	protected MarketDataBuffer buffer;
	
	public GlobalMarketRegistry(FastEventEngine feEngine, Consumer<NormalContract> onContractSave, Consumer<ContractField> onContractSubsciption,
			MarketDataBuffer buffer) {
		this.feEngine = feEngine;
		this.onContractSave = onContractSave;
		this.onContractSubsciption = onContractSubsciption;
		this.buffer = buffer;
	}
	
	public synchronized void register(NormalContract contract) {
		onContractSave.accept(contract);
		if(subMgrMap.containsKey(contract.gatewayType()) && !subMgrMap.get(contract.gatewayType()).subscribable(contract)) {
			log.trace("订阅管理器跳过订阅 [{} -> {}] 合约", contract.gatewayType(), contract.unifiedSymbol());
			return;
		}
		
		onContractSubsciption.accept(contract.contractField());
		contractMap.put(contract.unifiedSymbol(), contract);
		makeBarGen(contract);
		if(contract instanceof IndexContract idxContract) {
			log.debug("注册指数合约：{}", contract.unifiedSymbol());
			makeTicker(idxContract);
			return;		// 指数合约不需要订阅，因此无需继续后面步骤
		}
		
		// 合约订阅
		log.debug("注册普通合约：{}", contract.unifiedSymbol());
		if(gatewayMap.containsKey(contract.gatewayType())) {
			doSubscribe(contract);
		}
	}
	
	// 设置BAR回调
	private void makeBarGen(NormalContract contract) {
		BarGenerator barGen = contract.barGenerator();
		barGen.setOnBarCallback((bar, ticks) -> {
			feEngine.emitEvent(NorthstarEventType.BAR, bar);
			buffer.save(bar, ticks);
		});
		barGenMap.put(contract.unifiedSymbol(), barGen);
	}
	
	// 设置TICK回调
	private void makeTicker(IndexContract idxContract) {
		IndexTicker ticker = idxContract.indexTicker();
		ticker.setOnTickCallback(tick -> {
			feEngine.emitEvent(NorthstarEventType.TICK, tick);
			dispatch(tick);
		});
		ticker.dependencySymbols().forEach(unifiedSymbol -> idxTickerMap.put(unifiedSymbol, ticker));
	}
	
	public synchronized void register(SubscriptionManager subscriptionManager) {
		subMgrMap.put(subscriptionManager.usedFor(), subscriptionManager);
	}
	
	public synchronized void register(MarketGateway mktGateway) {
		gatewayMap.put(mktGateway.gatewayType(), mktGateway);
		if(mktGateway.isConnected()) {
			autoSubscribeContracts(mktGateway.gatewayType());
		}
	}
	
	public synchronized void unregister(MarketGateway mktGateway) {
		gatewayMap.remove(mktGateway.gatewayType());
	}
	
	private void doSubscribe(NormalContract contract) {
		MarketGateway gateway = gatewayMap.get(contract.gatewayType());
		ContractField contractField = contract.contractField();
		if(gateway.isConnected()) {			
			gateway.subscribe(contractField);
		}
	}
	
	public synchronized void autoSubscribeContracts(GatewayType gatewayType) {
		if(!gatewayMap.containsKey(gatewayType)) {
			throw new IllegalStateException("没有注册相应的行情网关：" + gatewayType);
		}
		MarketGateway mktGateway = gatewayMap.get(gatewayType);
		if(!contractMap.isEmpty()) {
			contractMap.values().stream()
				.filter(c -> c.gatewayType() == mktGateway.gatewayType() && !(c instanceof IndexContract))
				.forEach(this::doSubscribe);
		}
	}
	
	public void dispatch(TickField tick) {
		NormalContract contract = contractMap.get(tick.getUnifiedSymbol());
		if(contract == null) {
			log.warn("没有登记合约 [{}]，忽略TICK分发", tick.getUnifiedSymbol());
			return;
		}
		if(!(contract instanceof IndexContract) && idxTickerMap.containsKey(tick.getUnifiedSymbol())) {
			idxTickerMap.get(tick.getUnifiedSymbol()).update(tick);
		}
		barGenMap.get(tick.getUnifiedSymbol()).update(tick);
	}
}
