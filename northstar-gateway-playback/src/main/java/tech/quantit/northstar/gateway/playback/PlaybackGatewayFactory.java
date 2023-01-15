package tech.quantit.northstar.gateway.playback;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import com.alibaba.fastjson.JSON;

import tech.quantit.northstar.common.IHolidayManager;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.PlaybackRuntimeDescription;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.gateway.playback.utils.CtpPlaybackClock;
import tech.quantit.northstar.gateway.playback.utils.PlaybackClock;
import tech.quantit.northstar.gateway.playback.utils.PlaybackDataLoader;

public class PlaybackGatewayFactory implements GatewayFactory{

	private IPlaybackRuntimeRepository rtRepo;
	
	private IHolidayManager holidayMgr;
	
	private FastEventEngine feEngine;
	
	private IMarketDataRepository mdRepo;
	
	private IContractManager contractMgr;
	
	public PlaybackGatewayFactory(FastEventEngine feEngine, IContractManager contractMgr, IHolidayManager holidayMgr,
			IPlaybackRuntimeRepository rtRepo, IMarketDataRepository mdRepo) {
		this.rtRepo = rtRepo;
		this.mdRepo = mdRepo;
		this.holidayMgr = holidayMgr;
		this.feEngine = feEngine;
		this.contractMgr = contractMgr;
	}
	
	@Override
	public Gateway newInstance(GatewayDescription gatewayDescription) {
		PlaybackRuntimeDescription playbackRt = rtRepo.findById(gatewayDescription.getGatewayId());
		PlaybackGatewaySettings settings = JSON.parseObject(JSON.toJSONString(gatewayDescription.getSettings()), PlaybackGatewaySettings.class);
		LocalDateTime ldt = Objects.nonNull(playbackRt) 
				? playbackRt.getPlaybackTimeState() 
				: LocalDateTime.of(LocalDate.parse(settings.getStartDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER), LocalTime.of(20, 0));
		PlaybackClock clock = new CtpPlaybackClock(holidayMgr, ldt);
		PlaybackDataLoader loader = new PlaybackDataLoader(gatewayDescription.getGatewayId(), mdRepo);
		PlaybackContext context = new PlaybackContext(gatewayDescription, ldt, clock, loader, feEngine, rtRepo, contractMgr);
		return new PlaybackGatewayAdapter(context, gatewayDescription);
	}
	
}
