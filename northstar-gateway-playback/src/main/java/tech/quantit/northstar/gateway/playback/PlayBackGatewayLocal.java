package tech.quantit.northstar.gateway.playback;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.PlaybackDescription;
import tech.quantit.northstar.gateway.api.MarketGateway;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import xyz.redtorch.pb.CoreField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.TickField;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.*;

/**
 * 回放GateWay
 *
 * @author changsong
 */
@Slf4j
public class PlayBackGatewayLocal implements MarketGateway{

	private FastEventEngine feEngine;

	private ScheduledExecutorService scheduleExec = Executors.newScheduledThreadPool(10);

	private long lastActiveTime;

	private GatewaySettingField settings;

	private ScheduledFuture<?> task;

	private MarketDataLocal marketDataLocal;

	private PlaybackDescription playbackDescription;

	/**
	 * unifiedSymbol --> InstrumentHolder
	 */
	// private ConcurrentMap<String, InstrumentHolder> cache = new ConcurrentHashMap<>();

	@Getter
	private boolean connected;

	private GlobalMarketRegistry registry;

	public PlayBackGatewayLocal(GatewaySettingField settings, FastEventEngine feEngine, GlobalMarketRegistry registry,
								PlaybackDescription playbackDescription) {
		this.feEngine = feEngine;
		this.settings = settings;
		this.registry = registry;
		this.playbackDescription = playbackDescription;
	}

	@Override
	public boolean subscribe(ContractField contract) {
		// cache.putIfAbsent(contract.getUnifiedSymbol(), new InstrumentHolder(contract));
		// log.info("模拟订阅合约：{}", contract.getSymbol());
		return true;
	}

	@Override
	public boolean unsubscribe(ContractField contract) {
		// cache.remove(contract.getUnifiedSymbol());
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
		log.info("回放行情连线");
		connected = true;
		feEngine.emitEvent(NorthstarEventType.CONNECTED, settings.getGatewayId());
		feEngine.emitEvent(NorthstarEventType.LOGGED_IN, settings.getGatewayId());

		int replayRate = playbackDescription.getReplayRate();
		// 取得回放数据
		Map<String, PriorityQueue<TickField>> tickData = marketDataLocal.getTickData();
		tickData.keySet().forEach(unifiedSymbol -> {
			PriorityQueue<CoreField.TickField> tickQ = (PriorityQueue<CoreField.TickField>) tickData.get(unifiedSymbol);
			task = scheduleExec.scheduleAtFixedRate(()->{
				lastActiveTime = System.currentTimeMillis();
				try {
					CoreField.TickField bar = tickQ.poll();
					log.info("开始回放数据：{} {} {}", bar.getUnifiedSymbol(), bar.getActionDay(), bar.getActionTime());

					feEngine.emitEvent(NorthstarEventType.TICK, bar);
					registry.dispatch(bar);
				} catch (Exception e) {
					log.error("回放行情TICK生成异常", e);
				}
			}, 500, replayRate, TimeUnit.MILLISECONDS);
		});

		feEngine.emitEvent(NorthstarEventType.CONNECTED, settings.getGatewayId());
	}

	@Override
	public void disconnect() {
		if(task != null) {
			task.cancel(false);
		}
		log.info("[{}] 回放行情断开", settings.getGatewayId());
		connected = false;
		feEngine.emitEvent(NorthstarEventType.DISCONNECTED, settings.getGatewayId());
		feEngine.emitEvent(NorthstarEventType.LOGGED_OUT, settings.getGatewayId());
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
		return GatewayType.PLAYBACK;
	}

}
