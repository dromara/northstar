package tech.quantit.northstar.gateway.sim.market;

import java.util.Map.Entry;
import java.util.concurrent.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.gateway.api.MarketGateway;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public class SimMarketGatewayLocal implements MarketGateway{

	private FastEventEngine feEngine;

	private ScheduledExecutorService scheduleExec = new ScheduledThreadPoolExecutor(0,
			new BasicThreadFactory.Builder().namingPattern("sim-market-gateway-%d").daemon(false).build());

	private long lastActiveTime;

	private GatewaySettingField settings;

	private ScheduledFuture<?> task;

	private SimTickGenerator tickGen = new SimTickGenerator();

	/**
	 * unifiedSymbol --> InstrumentHolder
	 */
	private ConcurrentMap<String, InstrumentHolder> cache = new ConcurrentHashMap<>();

	private GlobalMarketRegistry registry;

	public SimMarketGatewayLocal(GatewaySettingField settings, FastEventEngine feEngine, GlobalMarketRegistry registry) {
		this.feEngine = feEngine;
		this.settings = settings;
		this.registry = registry;
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
					TickField tick = tickGen.generateNextTick(e.getValue());
					feEngine.emitEvent(NorthstarEventType.TICK, tick);
					registry.dispatch(tick);
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

	@Override
	public GatewayType gatewayType() {
		return GatewayType.SIM;
	}

}
