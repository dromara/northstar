package org.dromara.northstar.gateway.sim.market;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.contract.GatewayContract;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimMarketGatewayLocal implements MarketGateway{
	
	private FastEventEngine feEngine;
	
	private long lastActiveTime;
	
	private Timer timer;
	
	private IMarketCenter mktCenter;
	
	private GatewayDescription gd;
	
	private ConnectionState connState = ConnectionState.DISCONNECTED;
	
	private Map<String, SimTickGenerator> tickGenMap;
	
	public SimMarketGatewayLocal(GatewayDescription gd, FastEventEngine feEngine, IMarketCenter mktCenter, Map<String, SimTickGenerator> tickGenMap) {
		this.feEngine = feEngine;
		this.mktCenter = mktCenter;
		this.gd = gd;
		this.tickGenMap = tickGenMap;
	}

	@Override
	public boolean subscribe(Contract contract) {
		log.info("模拟订阅合约：{}", contract.symbol());
		return true;
	}

	@Override
	public boolean unsubscribe(Contract contract) {
		log.info("模拟退订合约：{}", contract.symbol());
		return true;
	}

	@Override
	public boolean isActive() {
		return System.currentTimeMillis() - lastActiveTime < 1000;
	}

	@Override
	public void connect() {
		if(connState == ConnectionState.CONNECTED) {
			return;
		}
		log.info("模拟行情连线");
		timer = new Timer("sim", true);
		timer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				lastActiveTime = System.currentTimeMillis();
				try {				
					for(Entry<String, SimTickGenerator> e: tickGenMap.entrySet()) {
						Tick tick = e.getValue().generateNextTick(LocalDateTime.now(), SimMarketGatewayLocal.this);
						mktCenter.onTick(tick);
						feEngine.emitEvent(NorthstarEventType.TICK, tick);
						GatewayContract contract = (GatewayContract) mktCenter.getContract(ChannelType.SIM, tick.contract().unifiedSymbol());
						contract.onTick(tick);
					}
				} catch (Exception e) {
					log.error("模拟行情TICK生成异常", e);
				}				
			}
			
		}, 500L, 500L);
		
		connState = ConnectionState.CONNECTED;
		CompletableFuture.runAsync(() -> feEngine.emitEvent(NorthstarEventType.GATEWAY_READY, gd.getGatewayId()), 
				CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS));
	}

	@Override
	public void disconnect() {
		if(timer != null) {			
			timer.cancel();
			timer = null;
		}
		log.info("模拟行情断开");
		connState = ConnectionState.DISCONNECTED;
	}

	@Override
	public ConnectionState getConnectionState() {
		return connState;
	}
	 
	@Override
	public boolean getAuthErrorFlag() {
		return false;
	}

	@Override
	public ChannelType channelType() {
		return ChannelType.SIM;
	}

	@Override
	public GatewayDescription gatewayDescription() {
		gd.setConnectionState(getConnectionState());
		return gd;
	}

	@Override
	public String gatewayId() {
		return gd.getGatewayId();
	}

}
