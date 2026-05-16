package org.dromara.northstar.gateway.sim.market;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.mktdata.MarketCenter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimMarketGatewayLocal extends MarketGateway{
	
	private FastEventEngine feEngine;
	
	private AtomicLong lastActiveTime = new AtomicLong();
	
	private Timer timer;
	
	private MarketCenter mktCenter;
	
	private Map<String, SimTickGenerator> tickGenMap;
	
	private Set<SimTickGenerator> subContractsGen = new HashSet<>();
	
	public SimMarketGatewayLocal(GatewayDescription gd, FastEventEngine feEngine, MarketCenter mktCenter, Map<String, SimTickGenerator> tickGenMap) {
		super(gd);
		this.feEngine = feEngine;
		this.mktCenter = mktCenter;
		this.tickGenMap = tickGenMap;
	}

	@Override
	public boolean subscribe() {
		synchronized (subContractsGen) {			
			getDescription().getSubscribedContracts().forEach(csi -> {
				log.info("模拟订阅合约：{}", csi.getSymbol());
				if(tickGenMap.containsKey(csi.getUnifiedSymbol())) {
					subContractsGen.add(tickGenMap.get(csi.getUnifiedSymbol()));
				}
			});
		}
		return true;
	}

	@Override
	public boolean unsubscribe() {
		synchronized (subContractsGen) {
			getDescription().getSubscribedContracts().forEach(csi -> {
				log.info("模拟退订合约：{}", csi.getSymbol());
				if(tickGenMap.containsKey(csi.getUnifiedSymbol())) {
					subContractsGen.remove(tickGenMap.get(csi.getUnifiedSymbol()));
				}
			});
		}
		return true;
	}

	@Override
	public boolean isActive() {
		return System.currentTimeMillis() - lastActiveTime.get() < 3000;
	}

	@Override
	public void connect() {
		if(connection == ConnectionState.CONNECTED) {
			return;
		}
		log.info("模拟行情连线");
		timer = new Timer("sim", true);
		timer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				try {				
					synchronized (subContractsGen) {
						for(SimTickGenerator gen: subContractsGen) {
							lastActiveTime.set(System.currentTimeMillis()); 
							Tick tick = gen.generateNextTick(LocalDateTime.now(), SimMarketGatewayLocal.this);
							mktCenter.onTick(tick);
							feEngine.emitEvent(NorthstarEventType.TICK, tick);
							log.trace("模拟行情TICK：{} {} {} {}", tick.contract().name(), tick.actionDay(), tick.actionTime(), tick.lastPrice());
						}
					}
				} catch (Exception e) {
					log.error("模拟行情TICK生成异常", e);
				}				
			}
			
		}, 500L, 500L);
		
		connection = ConnectionState.CONNECTED;
		CompletableFuture.runAsync(() -> feEngine.emitEvent(NorthstarEventType.GATEWAY_READY, description.getGatewayId()), 
				CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS));
	}

	@Override
	public void disconnect() {
		if(timer != null) {			
			timer.cancel();
			timer = null;
		}
		log.info("模拟行情断开");
		connection = ConnectionState.DISCONNECTED;
		feEngine.emitEvent(NorthstarEventType.LOGGED_OUT, description.getGatewayId());
	}

}
