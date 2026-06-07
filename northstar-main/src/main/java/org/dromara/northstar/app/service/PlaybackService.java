package org.dromara.northstar.app.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.PlaybackStatusVO;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.gateway.playback.PlaybackContext;
import org.dromara.northstar.gateway.playback.PlaybackGatewayAdapter;
import org.dromara.northstar.gateway.playback.PlaybackStatus;
import org.dromara.northstar.gateway.playback.PlaybackStatus.State;
import org.dromara.northstar.kline.BarMerger;
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.strategy.MergedBarListener;
import org.dromara.northstar.support.utils.EventEmitterHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PlaybackService {

	private static final Identifier ID = Identifier.of("PLAYBACK");
	
	@Autowired
	private GatewayManager gatewayMgr;
	
	@Autowired
	private ModuleManager moduleMgr;
	
	@Autowired
	private ModuleService moduleService;
	
	@Autowired
	private MarketDataService dataService;
	
	@Autowired
	private MarketCenter mktCenter;
	
	@Autowired
	private EventEmitterHolder emitterHolder;
	
	private AtomicLong lastStatusSent = new AtomicLong();
	
	private Set<State> playingStates = Set.of(State.PRELOADING, State.PLAYING);
	
	private Consumer<PlaybackStatus> onStatusChangeCallback = status -> {
		if(playingStates.contains(status.getState()) && System.currentTimeMillis() - lastStatusSent.get() < 100) {
			// 自动过滤状态回报，避免IO拥挤
			return;
		}
		PlaybackStatusVO vo = PlaybackStatusVO.builder()
				.state(status.getState().toString())
				.speed(500 - status.getPlayInterval())
				.playingTimestamp(CommonUtils.localDateTimeToMills(status.getRunningDateTime()))
				.build();
		emitterHolder.send(SseEmitter.event().name("PLAYBACK_STATUS").data(vo));
		lastStatusSent.set(System.currentTimeMillis());
	};
	
	public void start() {
		getPlaybackContext().start();
	}
	
	public void pause() {
		getPlaybackContext().pause();
	}
	
	public void reset() {
		getPlaybackContext().reset();
		
		moduleMgr.findAll().forEach(m -> {
			if(m.getModuleContext().getModuleDescription().getUsage() == ModuleUsage.PLAYBACK) {
				log.info("回放状态重置，需要同时重置所有回测模组");
				ModuleDescription md = m.getModuleContext().getModuleDescription();
				try {
					moduleService.updateModule(md, true);
				} catch (Exception e) {
					log.error("", e);
				}
			}
		});
	}
	
	public void preload() {
		getPlaybackContext().preload();
	}
	
	public PlaybackStatusVO getPlaybackStatus() {
		PlaybackStatus pstatus = getPlaybackContext().getPlaybackStatus();
		return PlaybackStatusVO.builder()
				.speed(500 - pstatus.getPlayInterval())
				.state(pstatus.getState().toString())
				.playingTimestamp(CommonUtils.localDateTimeToMills(pstatus.getRunningDateTime()))
				.build();
	}
	
	public void setSpeed(int interval) {
		getPlaybackContext().setSpeed(interval);
	}
	
	public void setTime(long timestamp) {
		getPlaybackContext().getPlaybackStatus().setRunningDateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));
	}
	
	public void playNext(Interval interval) {
		getPlaybackContext().playNext(interval);
	}
	
	public List<Bar> getRangeBars(String contractId, Interval interval, long from, long to){
		long playbackTime = CommonUtils.localDateTimeToMills(getPlaybackContext().getPlaybackStatus().getRunningDateTime());
		if(from >= playbackTime) {
			return Collections.emptyList();
		}
		return dataService.getRangeBars(contractId, interval, from, Math.min(playbackTime, to));
	}
	
	public List<Bar> getPreloadBars(String contractId, Interval interval){
		log.info("预加载数据，合约{}，周期{} ", contractId, interval.getAlias());
		
		// 预加载数据时，需要先暂停回放进度
		Contract contract = mktCenter.getContract(Identifier.of(contractId)).contract();
		getPlaybackContext().resetBarMerger(contract, interval);
		BarMerger merger = getPlaybackContext().getBarMerger(contract, interval);
		getPlaybackContext().suspend();
		if(getPlaybackContext().getPlaybackStatus().getState() == PlaybackStatus.State.INITIAL) {
			getPlaybackContext().getPlaybackStatus().setState(PlaybackStatus.State.PRELOADED);
		}
		long to = CommonUtils.localDateTimeToMills(getPlaybackContext().getPlaybackStatus().getRunningDateTime());
		long endTime = to;
		LocalDateTime toDateTime = CommonUtils.millsToLocalDateTime(to);	
		int dayDif = switch (interval) {
			case M1,M3 -> 3;
			case M5 -> 5;
			case M10 -> 10;
			case M15 -> 15;
			case M30 -> 30;
			case H1 -> 60;
			case H2 -> 120;
			case H4 -> 240;
			case D1,WK1 -> 300;
			default -> throw new IllegalArgumentException("不支持的周期类型：" + interval);
		};
		LocalDateTime fromDateTime = getPlaybackContext().getPlaybackStatus().getRunningDateTime().minusDays(dayDif);
		List<Bar> mergedBars = new LinkedList<>();
		MergedBarListener tempListener = mergedBars::add;
		merger.addListener(tempListener);
		List<Bar> preloadRaw = dataService.getRangeBars(contractId, interval, CommonUtils.localDateTimeToMills(fromDateTime), CommonUtils.localDateTimeToMills(toDateTime))
			.stream()
			.filter(bar -> bar.actionTimestamp() <= endTime)
			.toList();
		List<Bar> preloadRefine = dataService.getRangeBars(contractId, Interval.M1, preloadRaw.getLast().actionTimestamp()+1, CommonUtils.localDateTimeToMills(toDateTime))
				.stream()
				.filter(bar -> bar.actionTimestamp() <= endTime)
				.toList();
		preloadRaw.forEach(merger::onBar);
		preloadRefine.forEach(merger::onBar);
		getPlaybackContext().resume();
		return mergedBars;
	}
	
	private PlaybackContext getPlaybackContext() {
		PlaybackGatewayAdapter playbackGateway = (PlaybackGatewayAdapter) gatewayMgr.get(ID);
		Assert.notNull(playbackGateway, "PLAYBACK网关不存在");
		playbackGateway.getContext().setOnStatusChangeCallback(onStatusChangeCallback);
		return playbackGateway.getContext();
	}
	
}
