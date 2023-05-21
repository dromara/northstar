package org.dromara.northstar.gateway.playback;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import org.dromara.northstar.common.IHolidayManager;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.PlaybackRuntimeDescription;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.GatewayFactory;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.playback.utils.CtpPlaybackClock;
import org.dromara.northstar.gateway.playback.utils.PlaybackClock;
import org.dromara.northstar.gateway.playback.utils.PlaybackDataLoader;
import org.dromara.northstar.gateway.utils.MarketDataRepoFactory;

import com.alibaba.fastjson.JSON;

public class PlaybackGatewayFactory implements GatewayFactory{

	private IPlaybackRuntimeRepository rtRepo;
	
	private IHolidayManager holidayMgr;
	
	private FastEventEngine feEngine;
	
	private MarketDataRepoFactory mdRepoFactory;
	
	private IContractManager contractMgr;
	
	public PlaybackGatewayFactory(FastEventEngine feEngine, IContractManager contractMgr, IHolidayManager holidayMgr,
			IPlaybackRuntimeRepository rtRepo, MarketDataRepoFactory mdRepoFactory) {
		this.rtRepo = rtRepo;
		this.mdRepoFactory = mdRepoFactory;
		this.holidayMgr = holidayMgr;
		this.feEngine = feEngine;
		this.contractMgr = contractMgr;
	}
	
	@Override
	public Gateway newInstance(GatewayDescription gatewayDescription) {
		PlaybackRuntimeDescription playbackRt = rtRepo.findById(gatewayDescription.getGatewayId());
		PlaybackGatewaySettings settings = JSON.parseObject(JSON.toJSONString(gatewayDescription.getSettings()), PlaybackGatewaySettings.class);
		gatewayDescription.setSettings(settings);
		LocalDateTime ldt = Objects.nonNull(playbackRt) 
				? playbackRt.getPlaybackTimeState() 
				: LocalDateTime.of(LocalDate.parse(settings.getStartDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER), LocalTime.of(20, 0));
		PlaybackClock clock = new CtpPlaybackClock(holidayMgr, ldt);
		PlaybackDataLoader loader = new PlaybackDataLoader(gatewayDescription.getGatewayId(), mdRepoFactory);
		PlaybackContext context = new PlaybackContext(gatewayDescription, ldt, clock, loader, feEngine, rtRepo, contractMgr);
		return new PlaybackGatewayAdapter(context, gatewayDescription);
	}
	
}
