package tech.quantit.northstar.gateway.playback;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import com.alibaba.fastjson.JSON;

import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.common.IHolidayManager;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.PlaybackRuntimeDescription;
import tech.quantit.northstar.common.model.PlaybackSettings;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.playback.ticker.TickSimulationAlgorithm;
import tech.quantit.northstar.gateway.playback.ticker.TrigonometricTickSimulation;
import tech.quantit.northstar.gateway.playback.utils.CtpPlaybackClock;
import tech.quantit.northstar.gateway.playback.utils.PlaybackClock;
import tech.quantit.northstar.gateway.playback.utils.PlaybackDataLoader;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

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
		PlaybackSettings settings = JSON.parseObject(JSON.toJSONString(gatewayDescription.getSettings()), PlaybackSettings.class);
		
		PlaybackContext context = createPlaybackContext(gatewayDescription.getGatewayId(), settings, playbackRt);
		GatewaySettingField settingField = createGatewaySettings(gatewayDescription);
		return new PlaybackGatewayAdapter(context, settingField);
	}
	
	private PlaybackContext createPlaybackContext(String gatewayId, PlaybackSettings settings, PlaybackRuntimeDescription playbackRt) {
		LocalDateTime ldt = Objects.nonNull(playbackRt) 
				? playbackRt.getPlaybackTimeState() 
				: LocalDateTime.of(LocalDate.parse(settings.getStartDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER), LocalTime.of(9, 0));
		PlaybackClock clock = new CtpPlaybackClock(holidayMgr, ldt);
		PlaybackDataLoader loader = new PlaybackDataLoader(mdRepo);
		TickSimulationAlgorithm ticker = new TrigonometricTickSimulation(gatewayId, settings.getPrecision(), contractMgr);
		return new PlaybackContext(settings, ldt, clock, ticker, loader, feEngine, rtRepo, contractMgr);
	}
	
	private GatewaySettingField createGatewaySettings(GatewayDescription gatewayDescription) {
		return GatewaySettingField.newBuilder()
				.setGatewayId(gatewayDescription.getGatewayId())
				.setGatewayType(GatewayTypeEnum.GTE_MarketData)
				.build();
	}
}
