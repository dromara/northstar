package org.dromara.northstar.gateway.sim.market;

import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.contract.GatewayContract;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public class SimMarketGatewayLocal implements MarketGateway{
	
	private FastEventEngine feEngine;
	
	private ScheduledExecutorService scheduleExec = Executors.newScheduledThreadPool(0);
	
	private long lastActiveTime;
	
	private ScheduledFuture<?> task;
	
	private SimTickGenerator tickGen = new SimTickGenerator();
	
	/**
	 * unifiedSymbol --> InstrumentHolder
	 */
	private ConcurrentMap<String, InstrumentHolder> cache = new ConcurrentHashMap<>();
	
	private IMarketCenter mktCenter;
	
	private GatewayDescription gd;
	
	private ConnectionState connState = ConnectionState.DISCONNECTED;
	
	public SimMarketGatewayLocal(GatewayDescription gd, FastEventEngine feEngine, IMarketCenter mktCenter) {
		this.feEngine = feEngine;
		this.mktCenter = mktCenter;
		this.gd = gd;
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
	public void connect() {
		if(connState == ConnectionState.CONNECTED) {
			return;
		}
		log.info("模拟行情连线");
		task = scheduleExec.scheduleAtFixedRate(()->{
			lastActiveTime = System.currentTimeMillis();
			try {				
				for(Entry<String, InstrumentHolder> e: cache.entrySet()) {
					TickField tick = tickGen.generateNextTick(e.getValue());
					feEngine.emitEvent(NorthstarEventType.TICK, tick);
					GatewayContract contract = (GatewayContract) mktCenter.getContract(ChannelType.SIM, tick.getUnifiedSymbol());
					contract.onTick(tick);
				}
			} catch (Exception e) {
				log.error("模拟行情TICK生成异常", e);
			}
		}, 500, 500, TimeUnit.MILLISECONDS);
		
		connState = ConnectionState.CONNECTED;
		CompletableFuture.runAsync(() -> feEngine.emitEvent(NorthstarEventType.GATEWAY_READY, gd.getGatewayId()), 
				CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS));
	}

	@Override
	public void disconnect() {
		if(task != null) {			
			task.cancel(false);
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
