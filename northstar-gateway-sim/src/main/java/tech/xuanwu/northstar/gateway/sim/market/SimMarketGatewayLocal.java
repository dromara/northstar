package tech.xuanwu.northstar.gateway.sim.market;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.gateway.api.MarketGateway;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

@Slf4j
public class SimMarketGatewayLocal implements MarketGateway{
	
	private FastEventEngine feEngine;
	
	private ScheduledExecutorService scheduleExec = Executors.newScheduledThreadPool(0);
	
	private long lastActiveTime;
	
	private GatewaySettingField settings;
	
	private ScheduledFuture<?> task;
	
	private SimTickGenerator tickGen = new SimTickGenerator();
	
	/**
	 * unifiedSymbol --> InstrumentHolder
	 */
	private ConcurrentMap<String, InstrumentHolder> cache = new ConcurrentHashMap<>();
	
	public SimMarketGatewayLocal(GatewaySettingField settings, FastEventEngine feEngine) {
		this.feEngine = feEngine;
		this.settings = settings;
		
	}

	@Override
	public boolean subscribe(ContractField contract) {
		cache.putIfAbsent(contract.getUnifiedSymbol(), new InstrumentHolder(contract));
		log.info("模拟订阅合约：{}", contract.getSymbol());
		return true;
	}

	@Override
	public boolean unsubscribe(ContractField contract) {
		cache.remove(contract.getUnifiedSymbol());
		log.info("模拟退订合约：{}", contract.getSymbol());
		return true;
	}

	@Override
	public boolean isActive() {
		return System.currentTimeMillis() - lastActiveTime < 1000;
	}

	@Override
	public GatewaySettingField getGatewaySetting() {
		return settings;
	}

	@Override
	public void connect() {
		if(isConnected()) {
			return;
		}
		log.info("模拟行情连线");
		task = scheduleExec.scheduleAtFixedRate(()->{
			lastActiveTime = System.currentTimeMillis();
			try {				
				for(Entry<String, InstrumentHolder> e: cache.entrySet()) {
					feEngine.emitEvent(NorthstarEventType.TICK, tickGen.generateNextTick(e.getValue()));
				}
			} catch (Exception e) {
				log.error("模拟行情TICK生成异常", e);
			}
		}, 500, 500, TimeUnit.MILLISECONDS);
		feEngine.emitEvent(NorthstarEventType.CONNECTED, settings.getGatewayId());
	}

	@Override
	public void disconnect() {
		if(task != null) {			
			task.cancel(false);
		}
		log.info("模拟行情断开");
		feEngine.emitEvent(NorthstarEventType.DISCONNECTED, settings.getGatewayId());
	}

	@Override
	public boolean isConnected() {
		return isActive();
	}

	@Override
	public boolean getAuthErrorFlag() {
		return false;
	}

}
