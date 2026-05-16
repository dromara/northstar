package org.dromara.northstar.event;

import java.util.Set;

import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.gateway.playback.GlobalPlaybackDataDetector;
import org.dromara.northstar.gateway.playback.PlaybackGatewayAdapter;
import org.dromara.northstar.gateway.playback.PlaybackStatus.State;
import org.dromara.northstar.kline.BarMerger;
import org.dromara.northstar.strategy.MergedBarListener;
import org.dromara.northstar.support.utils.EventEmitterHolder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import cn.hutool.core.codec.Base64;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.BarField;

@Slf4j
public class BarAggregationHandler extends AbstractEventHandler implements GenericEventHandler, MergedBarListener {
	
	private static final Identifier ID = Identifier.of("PLAYBACK");
	
	private EventEmitterHolder emitterHolder;

	private GatewayManager gatewayMgr;
	
	private Table<Contract, Interval, BarMerger> mergers = HashBasedTable.create();
	
	private GlobalPlaybackDataDetector playbackDataDetector;
	
	public BarAggregationHandler(EventEmitterHolder emitterHolder, GatewayManager gatewayMgr, GlobalPlaybackDataDetector playbackDataDetector) {
		this.emitterHolder = emitterHolder;
		this.gatewayMgr = gatewayMgr;
		this.playbackDataDetector = playbackDataDetector;
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventType == NorthstarEventType.TICK || eventType == NorthstarEventType.BAR;
	}

	@Override
	protected synchronized void doHandle(NorthstarEvent e) {
		if(e.getData() instanceof Tick tick) {
			mergers.values().forEach(merger -> merger.onTick(tick));
		} else if(e.getData() instanceof Bar bar) {
			mergers.values().forEach(merger -> merger.onBar(bar));
		}
	}
	
	public synchronized boolean containsMerger(Contract contract, Interval interval) {
		return mergers.contains(contract, interval);
	}
	
	public synchronized void register(Contract contract, Interval interval, BarMerger merger) {
		if(!containsMerger(contract, interval)) {			
			mergers.put(contract, interval, merger);
			merger.addListener(this);
		}
	}
	
	public synchronized Set<Contract> registeredContracts(){
		return mergers.rowKeySet();
	}
	
	public synchronized void unregister(Contract contract, Interval interval) {
		mergers.remove(contract, interval);
	}
	
	public synchronized void clearMerger() {
		mergers.clear();
	}

	@Override
	public synchronized void onMergedBar(Bar b) {
		BarField bar = b.toBarField();
		if(log.isTraceEnabled()) {			
			log.trace("BAR数据分发：[{} {} {} {} 仓：{} 量：{} 价：{}]", bar.getUnifiedSymbol(), bar.getActionDay(), bar.getActionTime(), bar.getActionTimestamp(),
					bar.getOpenInterestDelta(), bar.getVolumeDelta(), bar.getClosePrice());
		}
		
		if(b.channelType() == ChannelType.PLAYBACK) {
			playbackDataDetector.onBar(b);
			PlaybackGatewayAdapter playback = (PlaybackGatewayAdapter) gatewayMgr.get(ID);
			if(playback.getContext().getPlaybackStatus().getState() == State.PRELOADING) {
				return; // 自动忽略预热的数据分发
			}
		}
		emitterHolder.send(SseEmitter.event().name(NorthstarEventType.BAR.toString()).data(Base64.encode(bar.toByteArray())));
	}

}
