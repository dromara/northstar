package org.dromara.northstar.app.service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
	
	private AtomicReference<State> lastState = new AtomicReference<>();
	
	private Consumer<PlaybackStatus> onStatusChangeCallback = status -> {
		if(lastState.get() != null && lastState.get() == status.getState() && System.currentTimeMillis() - lastStatusSent.get() < 100) {
			return;
		}
		PlaybackStatusVO vo = PlaybackStatusVO.builder()
				.state(status.getState().toString())
				.speed(status.getPlayInterval())
				.playingTimestamp(CommonUtils.localDateTimeToMills(status.getRunningDateTime()))
				.build();
		emitterHolder.send(SseEmitter.event().name("PLAYBACK_STATUS").data(vo));
		lastStatusSent.set(System.currentTimeMillis());
		lastState.set(status.getState());
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
				.speed(pstatus.getPlayInterval())
				.state(pstatus.getState().toString())
				.playingTimestamp(CommonUtils.localDateTimeToMills(pstatus.getRunningDateTime()))
				.build();
	}
	
	public void setSpeed(int interval) {
		getPlaybackContext().setSpeed(interval);
	}
	
	public void setTime(long timestamp) {
		getPlaybackContext().setRunningTime(timestamp);
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
		
		// 预加载数据时，需要先暂停回放进度。因多图同时刷新时会存在并发问题，只能统一暂停，然后手动开始
		getPlaybackContext().pause();
		
		Contract contract = mktCenter.getContract(Identifier.of(contractId)).contract();
		getPlaybackContext().resetBarMerger(contract, interval);
		BarMerger merger = getPlaybackContext().getBarMerger(contract, interval);

		if(getPlaybackContext().getPlaybackStatus().getState() == PlaybackStatus.State.INITIAL) {
			getPlaybackContext().getPlaybackStatus().setState(PlaybackStatus.State.PRELOADED);
		}
		int dayDif = switch (interval) {
			case M1,M3 -> 3;
			case M5 -> 5;
			case M10 -> 10;
			case M15 -> 15;
			case M30 -> 30;
			case H1 -> 60;
			case H2 -> 120;
			case H4 -> 240;
			case D1 -> 300;
			case WK1 -> 700;
			default -> throw new IllegalArgumentException("不支持的周期类型：" + interval);
		};
		long endTime = CommonUtils.localDateTimeToMills(getPlaybackContext().getPlaybackStatus().getRunningDateTime());
		long startTime =  endTime - TimeUnit.DAYS.toMillis(dayDif);
		
		List<Bar> mergedBars = new LinkedList<>();
		MergedBarListener tempListener = mergedBars::add;
		merger.addListener(tempListener);
		dataService.getRangeBars(contractId, interval, startTime, endTime).forEach(merger::onBar);
		
		// 对于以收盘时间为K线时间的合约，当查询周期大于1分钟时，可能需要额外的数据进行精确计算
		if(interval != Interval.M1 && !mergedBars.isEmpty() && !contract.usingStartTimeInBarTime()) {			
			Interval refineLoadInterval = interval.getMinutes() < Interval.D1.getMinutes() ? Interval.M1 : Interval.H1;
			long refineLoadStart = mergedBars.getLast().actionTimestamp() + TimeUnit.MINUTES.toMillis(1);
			if(refineLoadStart < endTime) {				
				dataService.getRangeBars(contractId, refineLoadInterval, refineLoadStart, endTime).forEach(merger::onBar);
			}
		}
		
		return mergedBars;
	}
	
	private PlaybackContext getPlaybackContext() {
		PlaybackGatewayAdapter playbackGateway = (PlaybackGatewayAdapter) gatewayMgr.get(ID);
		Assert.notNull(playbackGateway, "PLAYBACK网关不存在");
		playbackGateway.getContext().setOnStatusChangeCallback(onStatusChangeCallback);
		return playbackGateway.getContext();
	}
	
}
