package org.dromara.northstar.gateway.playback;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

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

import com.alibaba.fastjson.JSON;

public class PlaybackGatewayFactory implements GatewayFactory{

	private IPlaybackRuntimeRepository rtRepo;
	
	private FastEventEngine feEngine;
	
	private PlaybackDataServiceManager dsMgr;
	
	private IContractManager contractMgr;
	
	public PlaybackGatewayFactory(FastEventEngine feEngine, IContractManager contractMgr, 
			IPlaybackRuntimeRepository rtRepo, PlaybackDataServiceManager dsMgr) {
		this.rtRepo = rtRepo;
		this.dsMgr = dsMgr;
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
		PlaybackClock clock = new CtpPlaybackClock(ldt);
		PlaybackDataLoader loader = new PlaybackDataLoader(gatewayDescription.getGatewayId(), dsMgr);
		PlaybackContext context = new PlaybackContext(gatewayDescription, ldt, clock, loader, feEngine, rtRepo, contractMgr);
		return new PlaybackGatewayAdapter(context, gatewayDescription);
	}
	
}
