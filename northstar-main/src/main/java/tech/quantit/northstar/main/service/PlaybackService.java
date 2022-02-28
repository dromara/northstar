package tech.quantit.northstar.main.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.lang.UUID;
import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.PlaybackDescription;
import tech.quantit.northstar.common.model.SimSettings;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.domain.gateway.GatewayConnection;
import tech.quantit.northstar.domain.strategy.ModuleManager;
import tech.quantit.northstar.domain.strategy.SandboxModuleManager;
import tech.quantit.northstar.domain.strategy.StrategyModule;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import tech.quantit.northstar.gateway.sim.persistence.SimAccountRepository;
import tech.quantit.northstar.gateway.sim.trade.SimGatewayFactory;
import tech.quantit.northstar.gateway.sim.trade.SimMarket;
import tech.quantit.northstar.gateway.sim.trade.SimTradeGateway;
import tech.quantit.northstar.main.ExternalJarListener;
import tech.quantit.northstar.main.factories.StrategyModuleFactory;
import tech.quantit.northstar.main.persistence.IMarketDataRepository;
import tech.quantit.northstar.main.persistence.ModuleRepository;
import tech.quantit.northstar.main.playback.PlaybackEngine;
import tech.quantit.northstar.main.playback.PlaybackStat;
import tech.quantit.northstar.main.playback.PlaybackStatRecord;
import tech.quantit.northstar.main.playback.PlaybackTask;
import tech.quantit.northstar.strategy.api.model.ModuleDealRecord;
import tech.quantit.northstar.strategy.api.model.ModuleInfo;
import tech.quantit.northstar.strategy.api.model.ModuleTradeRecord;

@Slf4j
public class PlaybackService {
	
	private PlaybackTask task;
	
	private IMarketDataRepository mdRepo;
	
	private ModuleRepository moduleRepo;

	private PlaybackEngine pbEngine;
	
	private SimGatewayFactory simGatewayFactory;
	
	private StrategyModuleFactory moduleFactory;
	
	private SandboxModuleManager sandboxMgr;
	
	private GatewayAndConnectionManager gatewayConnMgr;
	
	private SimMarket simMarket;
	
	private volatile boolean isRunning;
	
	public PlaybackService(FastEventEngine feEngine, SandboxModuleManager sandboxMgr, GatewayAndConnectionManager gatewayConnMgr, ContractManager contractMgr,
			ModuleRepository moduleRepo, IMarketDataRepository mdRepo, SimMarket simMarket, SimAccountRepository simAccRepo, GlobalMarketRegistry registry,
			ExternalJarListener extJarListener) {
		simGatewayFactory = new SimGatewayFactory(feEngine, simMarket, simAccRepo, registry);
		moduleFactory = new StrategyModuleFactory(gatewayConnMgr, contractMgr, moduleRepo, null, extJarListener);
		pbEngine = new PlaybackEngine(simMarket, sandboxMgr);
		this.sandboxMgr = sandboxMgr;
		this.gatewayConnMgr = gatewayConnMgr;
		this.mdRepo = mdRepo;
		this.moduleRepo = moduleRepo;
		this.simMarket = simMarket;
	}

	/**
	 * 开始回测
	 * @param playbackDescription
	 * @param moduleMgr
	 * @throws Exception
	 */
	public void play(PlaybackDescription playbackDescription, ModuleManager moduleMgr) throws Exception{
		isRunning = true;
		List<String> moduleNames = playbackDescription.getModuleNames();
		List<StrategyModule> playbackModules = new ArrayList<>();
		List<SimTradeGateway> simGateways = new ArrayList<>();
		
		for(String name : moduleNames) {
			// 清理模组旧有回测记录
			clearOutPlaybackRecord(name);
			// 获取原有模组
			StrategyModule originModule = moduleMgr.getModule(name);
			
			// 构造一个回测专用的账户网关
			int fee = playbackDescription.getFee();
			GatewayDescription gwDescription = GatewayDescription.builder()
					.gatewayId(Constants.PLAYBACK_GATEWAY + "_" + UUID.randomUUID().toString().substring(0, 4))
					.gatewayType(GatewayType.SIM)
					.gatewayUsage(GatewayUsage.TRADE)
					.bindedMktGatewayId(originModule.getBindedMktGatewayId())
					.settings(SimSettings.builder().fee(fee).build())
					.build();
			SimTradeGateway simTradeGateway = (SimTradeGateway) simGatewayFactory.newInstance(gwDescription);
			GatewayConnection conn = new GatewayConnection(gwDescription);
			gatewayConnMgr.createPair(conn, simTradeGateway);
			
			ModuleInfo moduleInfo = moduleRepo.findModuleInfo(name);
			moduleInfo.setEnabled(true);
			moduleInfo.setModuleName(moduleInfo.getModuleName() + Constants.PLAYBACK_MODULE_SUFFIX);
			moduleInfo.setAccountGatewayId(gwDescription.getGatewayId());
			
			StrategyModule module = moduleFactory.makeModule(moduleInfo, Collections.emptyList(), true);
			module.setSubmitOrderHandler(simTradeGateway::submitOrder);
			module.setCancelOrderHandler(simTradeGateway::cancelOrder);
			module.setDealRecordGenHandler(moduleRepo::saveDealRecord);
			module.setSavingTradeCallback(trade -> 
				moduleRepo.saveTradeRecord(ModuleTradeRecord.builder()
						.contractName(trade.getContract().getFullName())
						.actionTime(trade.getTradeTimestamp())
						.moduleName(name + Constants.PLAYBACK_MODULE_SUFFIX)
						.operation(FieldUtils.chn(trade.getDirection()) + FieldUtils.chn(trade.getOffsetFlag()))
						.price(trade.getPrice())
						.tradingDay(trade.getTradingDay())
						.volume(trade.getVolume())
						.build())
				);
			module.setDealRecordGenHandler(dealRecord -> log.debug(""));
			playbackModules.add(module);
			sandboxMgr.addModule(module);
			
			simTradeGateway.connect();
			simTradeGateway.moneyIO(playbackDescription.getPlaybackAccountInitialBalance());
			simGateways.add(simTradeGateway);
		}
		
		task = new PlaybackTask(playbackDescription, playbackModules, mdRepo);
		
		new Thread(()->{			
			try {
				Thread.sleep(500);
				pbEngine.play(task);
			} catch (InterruptedException e) {
				log.error("", e);
			}
			// 清理回测网关与模组副本
			for(StrategyModule module : playbackModules) {
				CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(() -> {	
					Gateway gateway = module.getGateway();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						log.error("", e);
					}
					module.setEnabled(false);
					simMarket.removeGateway(module.getBindedMktGatewayId(), (SimTradeGateway) gateway);
					sandboxMgr.removeModule(module.getName());
					gatewayConnMgr.removePair(gateway);
					log.info("回测模组副本已清理");
				});
			}
			isRunning = false;
			
			// 计算回测统计结果
			for(String name : moduleNames) {				
				List<ModuleDealRecord> dealRecords = getDealRecords(name);
				PlaybackStat stat = new PlaybackStat(playbackDescription, dealRecords);
				PlaybackStatRecord.PlaybackStatRecordBuilder statRecordBuilder = PlaybackStatRecord.builder()
						.moduleName(name)
						.sumOfProfit(stat.sumOfProfit())
						.sumOfCommission(stat.sumOfCommission())
						.timesOfTransaction(stat.timesOfTransaction())
						.duration(stat.duration())
						.yearlyEarningRate(stat.yearlyEarningRate())
						.stdOfProfit(stat.stdOfPlaybackProfits())
						.maxFallback(stat.maxFallback())
						.meanOfOccupiedMoney(stat.meanOfOccupiedMoney());
				try {
					statRecordBuilder.meanOf10TransactionsAvgProfit(stat.meanOfNTransactionsAvgProfit(10))
						.stdOf10TransactionsAvgProfit(stat.stdOfNTransactionsAvgProfit(10))
						.meanOf10TransactionsAvgWinningRate(stat.meanOfNTransactionsAvgWinningRate(10))
						.stdOf10TransactionsAvgWinningRate(stat.stdOfNTransactionsAvgWinningRate(10))
						.meanOf5TransactionsAvgProfit(stat.meanOfNTransactionsAvgProfit(5))
						.stdOf5TransactionsAvgProfit(stat.stdOfNTransactionsAvgProfit(5))
						.meanOf5TransactionsAvgWinningRate(stat.meanOfNTransactionsAvgWinningRate(5))
						.stdOf5TransactionsAvgWinningRate(stat.stdOfNTransactionsAvgWinningRate(5));
				}catch(Exception e) {
					statRecordBuilder.exceptionMessage("样本不足，部分统计值无法计算");
				}
				moduleRepo.savePlaybackStatRecord(statRecordBuilder.build());
			}
			log.info("完成回测统计结果计算");
		}).start();
	}
	
	private void clearOutPlaybackRecord(String moduleName) {
		moduleRepo.removeModulePosition(moduleName + Constants.PLAYBACK_MODULE_SUFFIX);
		moduleRepo.removeDealRecords(moduleName + Constants.PLAYBACK_MODULE_SUFFIX);
		moduleRepo.removeTradeRecords(moduleName + Constants.PLAYBACK_MODULE_SUFFIX);
	}
	
	/**
	 * 查询回测进度
	 * @return
	 */
	public int playProcess(){
		if(task == null) {
			throw new IllegalStateException("回测未开始");
		}
		return (int)(task.ratioOfProcess() * 100);
	}
	
	/**
	 * 查询回测账户总额
	 * @param moduleName
	 * @return
	 */
	public int playbackBalance(String moduleName) {
		if(task == null) {
			throw new IllegalStateException("回测未开始");
		}
		return ((SimTradeGateway)sandboxMgr.getModule(moduleName + Constants.PLAYBACK_MODULE_SUFFIX).getGateway()).moneyIO(0);
	}
	
	/**
	 * 获取模组回测交易历史
	 * @param moduleName
	 * @return
	 */
	public List<ModuleDealRecord> getDealRecords(String moduleName) {
		return moduleRepo.findDealRecords(moduleName + Constants.PLAYBACK_MODULE_SUFFIX);
	}
	
	/**
	 * 获取模组回测成交历史
	 * @param moduleName
	 * @return
	 */
	public List<ModuleTradeRecord> getTradeRecords(String moduleName){
		return moduleRepo.findTradeRecords(moduleName + Constants.PLAYBACK_MODULE_SUFFIX);
	}
	
	/**
	 * 查询回测就绪状态
	 * @return
	 */
	public boolean getPlaybackReadiness(){
		return !isRunning;
	}

	/**
	 * 查询回测统计结果
	 * @param moduleName
	 * @return
	 */
	public PlaybackStatRecord getPlaybackStatRecord(String moduleName) {
		return moduleRepo.getPlaybackStatRecord(moduleName);
	}
}
