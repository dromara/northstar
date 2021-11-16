package tech.xuanwu.northstar.main.service;

import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.lang.UUID;
import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.constant.Constants;
import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.GatewayUsage;
import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.common.model.PlaybackDescription;
import tech.xuanwu.northstar.common.model.PlaybackRecord;
import tech.xuanwu.northstar.common.model.SimSettings;
import tech.xuanwu.northstar.domain.GatewayAndConnectionManager;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.domain.TraderGatewayConnection;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.gateway.api.Gateway;
import tech.xuanwu.northstar.gateway.sim.trade.SimGatewayFactory;
import tech.xuanwu.northstar.gateway.sim.trade.SimMarket;
import tech.xuanwu.northstar.gateway.sim.trade.SimTradeGateway;
import tech.xuanwu.northstar.main.manager.ModuleManager;
import tech.xuanwu.northstar.main.manager.SandboxModuleManager;
import tech.xuanwu.northstar.main.persistence.MarketDataRepository;
import tech.xuanwu.northstar.main.persistence.ModuleRepository;
import tech.xuanwu.northstar.main.playback.PlaybackEngine;
import tech.xuanwu.northstar.main.playback.PlaybackTask;
import tech.xuanwu.northstar.strategy.common.StrategyModule;
import tech.xuanwu.northstar.strategy.common.StrategyModuleFactory;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleInfo;

@Slf4j
public class PlaybackService {
	
	private PlaybackTask task;
	
	private MarketDataRepository mdRepo;
	
	private ModuleRepository moduleRepo;

	private PlaybackEngine pbEngine;
	
	private SimGatewayFactory simGatewayFactory;
	
	private StrategyModuleFactory moduleFactory;
	
	private SandboxModuleManager sandboxMgr;
	
	private InternalEventBus eventBus;
	
	private GatewayAndConnectionManager gatewayConnMgr;
	
	private boolean isRunning;
	
	public PlaybackService(FastEventEngine feEngine, SandboxModuleManager sandboxMgr, GatewayAndConnectionManager gatewayConnMgr,
			ContractManager contractMgr, ModuleRepository moduleRepo, MarketDataRepository mdRepo, SimMarket simMarket, InternalEventBus eventBus) {
		simGatewayFactory = new SimGatewayFactory(feEngine, simMarket, contractMgr);
		moduleFactory = new StrategyModuleFactory(contractMgr, gatewayConnMgr);
		pbEngine = new PlaybackEngine(simMarket, sandboxMgr);
		this.sandboxMgr = sandboxMgr;
		this.gatewayConnMgr = gatewayConnMgr;
		this.eventBus = eventBus;
		this.mdRepo = mdRepo;
		this.moduleRepo = moduleRepo;
	}

	public void play(PlaybackDescription playbackDescription, ModuleManager moduleMgr) throws Exception{
		isRunning = true;
		List<String> moduleNames = playbackDescription.getModuleNames();
		List<StrategyModule> playbackModules = new ArrayList<>();
		List<SimTradeGateway> simGateways = new ArrayList<>();
		
		for(String name : moduleNames) {
			// 获取原有模组
			StrategyModule originModule = moduleMgr.getModule(name);
			// 构造一个回测专用的账户网关
			int tickOfFee = playbackDescription.getTickOfFee();
			GatewayDescription gwDescription = GatewayDescription.builder()
					.gatewayId(Constants.PLAYBACK_GATEWAY + "_" + UUID.randomUUID().toString().substring(0, 4))
					.gatewayType(GatewayType.SIM)
					.gatewayUsage(GatewayUsage.TRADE)
					.bindedMktGatewayId(originModule.getBindedMarketGatewayId())
					.settings(SimSettings.builder().ticksOfCommission(tickOfFee).build())
					.build();
			SimTradeGateway simTradeGateway = (SimTradeGateway) simGatewayFactory.newInstance(gwDescription);
			GatewayConnection conn = new TraderGatewayConnection(gwDescription, eventBus);
			gatewayConnMgr.createPair(conn, simTradeGateway);
			
			ModuleInfo moduleInfo = moduleRepo.findModuleInfo(name);
			moduleInfo.setEnabled(true);
			moduleInfo.setModuleName(moduleInfo.getModuleName() + Constants.PLAYBACK_MODULE_SUFFIX);
			moduleInfo.setAccountGatewayId(gwDescription.getGatewayId());
			
			StrategyModule module = moduleFactory.makeModule(moduleInfo, new ModuleStatus(moduleInfo.getModuleName()));
			playbackModules.add(module);
			sandboxMgr.addModule(module);
			
			simTradeGateway.connect();
			simTradeGateway.moneyIO(playbackDescription.getPlaybackAccountInitialBalance());
			simGateways.add(simTradeGateway);
		}
		
		task = new PlaybackTask(playbackDescription, playbackModules, mdRepo);
		
		new Thread(()->{			
			pbEngine.play(task);
			// 清理回测网关与模组副本
			for(StrategyModule module : playbackModules) {
				Gateway gateway = module.getGateway();
				gateway.disconnect();
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					log.error("", e);
				}
				module.toggleRunningState();
				sandboxMgr.removeModule(module.getName());
				gatewayConnMgr.removePair(gateway);
			}
			isRunning = false;
		}).start();
	}
	
	
	public int playProcess(){
		return (int)task.ratioOfProcess() * 100;
	}
	
	public int playbackBalance(String moduleName) {
		return ((SimTradeGateway)sandboxMgr.getModule(moduleName).getGateway()).moneyIO(0);
	}
	
	public PlaybackRecord playbackRecord(String moduleName){
		throw new UnsupportedOperationException("Not implemented");
	}
	
	public boolean getPlaybackReadiness(){
		return !isRunning;
	}
}
