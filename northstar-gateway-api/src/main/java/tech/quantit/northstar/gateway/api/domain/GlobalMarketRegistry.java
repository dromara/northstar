package tech.quantit.northstar.gateway.api.domain;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.gateway.api.MarketGateway;
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
	
	
	public GlobalMarketRegistry(FastEventEngine feEngine) {
		this.feEngine = feEngine;
	}
	
	public synchronized void register(NormalContract contract) {
		contractMap.put(contract.unifiedSymbol(), contract);
		// 设置BAR回调
		BarGenerator barGen = contract.barGenerator();
		barGen.setOnBarCallback(bar -> feEngine.emitEvent(NorthstarEventType.BAR, bar));
		barGenMap.put(contract.unifiedSymbol(), barGen);
		
		if(contract instanceof IndexContract idxContract) {
			// 设置TICK回调
			IndexTicker ticker = idxContract.indexTicker();
			ticker.setOnTickCallback(tick -> {
				feEngine.emitEvent(NorthstarEventType.TICK, tick);
				dispatch(tick);
			});
			ticker.dependencySymbols().forEach(unifiedSymbol -> idxTickerMap.put(contract.unifiedSymbol(), ticker));
			return;		// 指数合约不需要订阅，因此无需继续后面步骤
		}
		
		// 合约订阅
		if(gatewayMap.containsKey(contract.gatewayType())) {
			if(subMgrMap.containsKey(contract.gatewayType()) && !subMgrMap.get(contract.gatewayType()).subscribable(contract)) {
				log.debug("订阅管理器跳过订阅 [{} -> {}] 合约", contract.gatewayType(), contract.unifiedSymbol());
				return;
			}
			MarketGateway gateway = gatewayMap.get(contract.gatewayType());
			gateway.subscribe(contract.contractField());
		}
	}
	
	public synchronized void register(SubscriptionManager subscriptionManager) {
		subMgrMap.put(subscriptionManager.usedFor(), subscriptionManager);
	}
	
	public synchronized void register(MarketGateway mktGateway) {
		gatewayMap.put(mktGateway.gatewayType(), mktGateway);
		if(contractMap.size() > 0) {
			contractMap.values().stream()
				.filter(c -> c.gatewayType() == mktGateway.gatewayType() && !(c instanceof IndexContract))
				.forEach(c -> mktGateway.subscribe(c.contractField()));
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
