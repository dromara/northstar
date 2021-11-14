package tech.xuanwu.northstar.main.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import cn.hutool.core.lang.UUID;
import tech.xuanwu.northstar.common.constant.Constants;
import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.GatewayUsage;
import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.common.model.PlaybackDescription;
import tech.xuanwu.northstar.common.model.PlaybackRecord;
import tech.xuanwu.northstar.domain.GatewayAndConnectionManager;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.gateway.sim.trade.SimGatewayFactory;
import tech.xuanwu.northstar.gateway.sim.trade.SimMarket;
import tech.xuanwu.northstar.gateway.sim.trade.SimTradeGateway;
import tech.xuanwu.northstar.main.manager.ModuleManager;
import tech.xuanwu.northstar.main.persistence.MarketDataRepository;
import tech.xuanwu.northstar.main.persistence.ModuleRepository;
import tech.xuanwu.northstar.main.playback.PlaybackEngine;
import tech.xuanwu.northstar.main.playback.PlaybackTask;
import tech.xuanwu.northstar.strategy.common.StrategyModule;
import tech.xuanwu.northstar.strategy.common.StrategyModuleFactory;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleInfo;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleRealTimeInfo;

public class PlaybackService {
	
	private PlaybackTask task;
	
	private MarketDataRepository mdRepo;
	
	private ModuleRepository moduleRepo;

	private PlaybackEngine pbEngine;
	
	private Map<String, SimTradeGateway> simGatewayMap;
	
	private SimGatewayFactory simGatewayFactory;
	
	private StrategyModuleFactory moduleFactory;
	
	private ModuleManager moduleMgr;
	
	public PlaybackService(FastEventEngine feEngine, ModuleManager moduleMgr, GatewayAndConnectionManager gatewayConnMgr, ContractManager contractMgr,
			ModuleRepository moduleRepo, MarketDataRepository mdRepo, SimMarket simMarket) {
		simGatewayFactory = new SimGatewayFactory(feEngine, simMarket, contractMgr);
		moduleFactory = new StrategyModuleFactory(contractMgr, gatewayConnMgr);
		pbEngine = new PlaybackEngine(simMarket, moduleMgr);
		this.moduleMgr = moduleMgr;
		this.mdRepo = mdRepo;
		this.moduleRepo = moduleRepo;
	}

	public void play(PlaybackDescription playbackDescription) throws Exception{
		simGatewayMap = new HashMap<>();
		List<String> moduleNames = playbackDescription.getModuleNames();
		List<StrategyModule> playbackModules = new ArrayList<>();
		List<SimTradeGateway> simGateways = new ArrayList<>();
		for(String name : moduleNames) {
			ModuleInfo moduleInfo = moduleRepo.findModuleInfo(name);
			moduleInfo.setModuleName(moduleInfo.getModuleName() + Constants.PLAYBACK_MODULE_SUFFIX);
			StrategyModule module = moduleFactory.makeModule(moduleInfo, new ModuleStatus(moduleInfo.getModuleName()));
			playbackModules.add(module);
			SimTradeGateway simTradeGateway = createSimGateway();
			simGateways.add(simTradeGateway);
			simGatewayMap.put(name, simTradeGateway);
			moduleMgr.addModule(module);
		}
		task = new PlaybackTask(playbackDescription, playbackModules, mdRepo);
		pbEngine.play(task);
	}
	
	private SimTradeGateway createSimGateway() {
		GatewayDescription gwDescription = GatewayDescription.builder()
				.gatewayId("PlaybackSimAccount_" + UUID.randomUUID())
				.gatewayType(GatewayType.SIM)
				.gatewayUsage(GatewayUsage.TRADE)
				.autoConnect(true)
				.bindedMktGatewayId(Constants.PLAYBACK_GATEWAY)
				.build();
		return (SimTradeGateway) simGatewayFactory.newInstance(gwDescription);
	}
	
	public int playProcess(){
		return (int)task.ratioOfProcess() * 100;
	}
	
	public int playbackBalance(String moduleName) {
		if(!simGatewayMap.containsKey(moduleName)) {			
			throw new NoSuchElementException("没有找到该回测模组：" + moduleName);
		}
		return simGatewayMap.get(moduleName).moneyIO(0);
	}
	
	public PlaybackRecord playbackRecord(String moduleName){
		return null;
	}
	
	public boolean getPlaybackReadiness(){
		return task == null || task.isDone();
	}
}
