package tech.quantit.northstar.gateway.api.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.gateway.api.MarketDataBuffer;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 全局市场注册中心
 * 负责路由行情数据到合适的BAR生成器或指数生成器
 * @author KevinHuangwl
 *
 */
@Slf4j
public class GlobalMarketRegistry {

	protected FastEventEngine feEngine;
	
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
	
	public GlobalMarketRegistry(FastEventEngine feEngine, Consumer<NormalContract> onContractSave, Consumer<ContractField> onContractSubsciption) {
		this.feEngine = feEngine;
		this.onContractSave = onContractSave;
		this.onContractSubsciption = onContractSubsciption;
//		this.buffer = buffer;
	}
	
	public synchronized void register(NormalContract contract) {
		onContractSave.accept(contract);
		onContractSubsciption.accept(contract.contractField());
		contractMap.put(contract.unifiedSymbol(), contract);
		makeBarGen(contract);
		if(contract instanceof IndexContract idxContract) {
			log.debug("注册指数合约：{}", contract.unifiedSymbol());
			makeTicker(idxContract);
		}
	}
	
	// 设置BAR回调
	private void makeBarGen(NormalContract contract) {
		BarGenerator barGen = contract.barGenerator();
		barGen.setOnBarCallback((bar, ticks) -> {
			feEngine.emitEvent(NorthstarEventType.BAR, bar);
//			buffer.save(bar, ticks);
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
	
	public void finishUpBarGen() {
		barGenMap.values().forEach(BarGenerator::finishOfBar);
	}
}
