package org.dromara.northstar.app.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.model.BarVO;
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
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.strategy.MergedBarListener;
import org.dromara.northstar.support.utils.EventEmitterHolder;
import org.dromara.northstar.support.utils.bar.BarMerger;
import org.dromara.northstar.support.utils.bar.BarMergerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.BarCycleEnum;

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
	
	public void playNext(BarCycleEnum period) {
		getPlaybackContext().playNext(period);
	}
	
	public List<BarVO> getRangeBars(String contractId, BarCycleEnum barPeriod, long from, long to){
		long playbackTime = CommonUtils.localDateTimeToMills(getPlaybackContext().getPlaybackStatus().getRunningDateTime());
		if(from >= playbackTime) {
			return Collections.emptyList();
		}
		return dataService.getRangeBars(contractId, barPeriod, from, Math.min(playbackTime, to));
	}
	
	public List<BarVO> getPreloadBars(String contractId, BarCycleEnum barPeriod){
		log.info("预加载数据，合约{}，周期{} ", contractId, barPeriod);
		
		// 预加载数据时，需要先暂停回放进度
		Contract contract = mktCenter.getContract(Identifier.of(contractId)).contract();
		BarMerger merger = BarMergerFactory.newInstance(barPeriod, contract);
		getPlaybackContext().suspend();
		if(getPlaybackContext().getPlaybackStatus().getState() == PlaybackStatus.State.INITIAL) {
			getPlaybackContext().getPlaybackStatus().setState(PlaybackStatus.State.PRELOADED);
		}
		long to = CommonUtils.localDateTimeToMills(getPlaybackContext().getPlaybackStatus().getRunningDateTime());
		long endTime = to;
		LocalDate toDate = CommonUtils.millsToLocalDateTime(to).toLocalDate().plusDays(3);	// 考虑到CTP周五夜盘的情况，因此要多加载三天的冗余数据
		LocalDate fromDate = null;
		switch(barPeriod) {
		case B_1Min, B_3Min -> {fromDate = toDate.minusWeeks(1).plusDays(1);}
		case B_5Min,B_10Min -> {fromDate = toDate.minusWeeks(2).plusDays(1);}
		case B_15Min, B_30Min -> {fromDate = toDate.minusWeeks(3).plusDays(1);}
		case B_1Hr, B_2Hr -> {fromDate = toDate.minusWeeks(12).plusDays(1);}
		case B_4Hr, B_1Day -> {fromDate = toDate.minusYears(1).plusDays(1);}
		case B_1Wk -> {fromDate = toDate.minusYears(2).plusDays(1);}
		default -> throw new IllegalArgumentException("未定义周期处理：" + barPeriod);
		};
		
		List<Bar> mergedBars = new LinkedList<>();
		MergedBarListener tempListener = mergedBars::add;
		merger.addListener(tempListener);
		dataService.smartLoad(contract, barPeriod, contract.contractDefinition().dataSource(), fromDate, toDate)
			.stream()
			.filter(bar -> bar.actionTimestamp() <= endTime)
			.forEach(merger::onBar);
		List<BarVO> resultList = mergedBars.stream().map(bar -> toVO(bar, barPeriod)).toList();
		getPlaybackContext().resume();
		return resultList;
	}
	
	private BarVO toVO(Bar bar, BarCycleEnum barPeriod) {
		return BarVO.builder()
				.timestamp(bar.actionTimestamp())
				.open(bar.openPrice())
				.close(bar.closePrice())
				.high(bar.highPrice())
				.low(bar.lowPrice())
				.volume(bar.volumeDelta())
				.oi(bar.openInterest())
				.tradingDay(bar.tradingDay().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.period(barPeriod)
				.build();
	}
	
	private PlaybackContext getPlaybackContext() {
		PlaybackGatewayAdapter playbackGateway = (PlaybackGatewayAdapter) gatewayMgr.get(ID);
		Assert.notNull(playbackGateway, "PLAYBACK网关不存在");
		playbackGateway.getContext().setOnStatusChangeCallback(onStatusChangeCallback);
		return playbackGateway.getContext();
	}
	
}
