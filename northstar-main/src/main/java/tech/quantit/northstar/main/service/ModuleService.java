package tech.quantit.northstar.main.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.StatUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.domain.gateway.GatewayConnection;
import tech.quantit.northstar.domain.strategy.ModuleManager;
import tech.quantit.northstar.domain.strategy.ModuleStatus;
import tech.quantit.northstar.domain.strategy.StrategyModule;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.main.ExternalJarListener;
import tech.quantit.northstar.main.factories.StrategyModuleFactory;
import tech.quantit.northstar.main.persistence.MarketDataRepository;
import tech.quantit.northstar.main.persistence.ModuleRepository;
import tech.quantit.northstar.main.persistence.po.MinBarDataPO;
import tech.quantit.northstar.main.persistence.po.ModulePositionPO;
import tech.quantit.northstar.strategy.api.DealerPolicy;
import tech.quantit.northstar.strategy.api.DynamicParamsAware;
import tech.quantit.northstar.strategy.api.RiskControlRule;
import tech.quantit.northstar.strategy.api.SignalPolicy;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.model.ComponentField;
import tech.quantit.northstar.strategy.api.model.ComponentMetaInfo;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import tech.quantit.northstar.strategy.api.model.ModuleDealRecord;
import tech.quantit.northstar.strategy.api.model.ModuleInfo;
import tech.quantit.northstar.strategy.api.model.ModulePositionInfo;
import tech.quantit.northstar.strategy.api.model.ModuleRealTimeInfo;
import tech.quantit.northstar.strategy.api.model.ModuleTradeRecord;
import tech.quantit.northstar.strategy.api.model.TimeSeriesData;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
public class ModuleService implements InitializingBean {

	private ApplicationContext ctx;

	private ModuleRepository moduleRepo;

	private MarketDataRepository mdRepo;

	private ModuleManager mdlMgr;

	private GatewayAndConnectionManager gatewayConnMgr;

	private ContractManager contractMgr;

	private StrategyModuleFactory moduleFactory;
	
	private ExternalJarListener extJarListener;

	public ModuleService(ApplicationContext ctx, ModuleRepository moduleRepo, MarketDataRepository mdRepo, ExternalJarListener extJarListener,
			ModuleManager mdlMgr, GatewayAndConnectionManager gatewayConnMgr, ContractManager contractMgr) {
		this.ctx = ctx;
		this.moduleRepo = moduleRepo;
		this.mdRepo = mdRepo;
		this.mdlMgr = mdlMgr;
		this.gatewayConnMgr = gatewayConnMgr;
		this.contractMgr = contractMgr;
		this.extJarListener = extJarListener;
		this.moduleFactory = new StrategyModuleFactory(gatewayConnMgr, contractMgr, moduleRepo);
	}

	/**
	 * 查询可选的信号策略
	 * 
	 * @return
	 */
	public List<ComponentMetaInfo> getRegisteredSignalPolicies() {
		return getComponentMeta(SignalPolicy.class);
	}

	/**
	 * 查询可选的风控规则
	 * 
	 * @return
	 */
	public List<ComponentMetaInfo> getRegisteredRiskControlRules() {
		return getComponentMeta(RiskControlRule.class);
	}

	/**
	 * 查询可选的交易策略
	 * 
	 * @return
	 */
	public List<ComponentMetaInfo> getRegisteredDealers() {
		return getComponentMeta(DealerPolicy.class);
	}

	private List<ComponentMetaInfo> getComponentMeta(Class<?> clz) {
		Map<String, Object> objMap = ctx.getBeansWithAnnotation(StrategicComponent.class);
		List<ComponentMetaInfo> result = new ArrayList<>(objMap.size());
		for (Entry<String, Object> e : objMap.entrySet()) {
			if (clz.isAssignableFrom(e.getValue().getClass())) {
				StrategicComponent anno = e.getValue().getClass().getAnnotation(StrategicComponent.class);
				result.add(new ComponentMetaInfo(anno.value(), e.getValue().getClass().getName()));
			}
		}
		return result;
	}

	/**
	 * 获取组件参数
	 * 
	 * @param name
	 * @return
	 * @throws ClassNotFoundException
	 */
	public Map<String, ComponentField> getComponentParams(ComponentMetaInfo info) throws ClassNotFoundException {
		String className = info.getClassName();
		Class<?> clz = null;
		ClassLoader cl = extJarListener.getExternalClassLoader();
		if(cl != null) {
			clz = cl.loadClass(className);
		}
		if(clz == null) {			
			clz = Class.forName(className);
		}
		DynamicParamsAware aware = (DynamicParamsAware) ctx.getBean(clz);
		DynamicParams params = aware.getDynamicParams();
		return params.getMetaInfo();
	}

	/**
	 * 新增模组
	 * 
	 * @param module
	 * @param shouldSave
	 * @throws Exception
	 */
	public boolean createModule(ModuleInfo info) throws Exception {
		loadModule(info, Collections.emptyList());
		return moduleRepo.saveModuleInfo(info);
	}

	/**
	 * 更新模组
	 * 
	 * @param info
	 * @throws Exception
	 */
	public boolean updateModule(ModuleInfo info) throws Exception {
		mdlMgr.removeModule(info.getModuleName());
		ModulePositionPO po = moduleRepo.loadModulePosition(info.getModuleName());
		List<ModulePositionInfo> posInfos = po == null ? Collections.emptyList() : po.getPositions();
		moduleRepo.deleteModuleInfoById(info.getModuleName());
		loadModule(info, posInfos);
		return moduleRepo.saveModuleInfo(info);
	}

	/**
	 * 加载模组
	 * 
	 * @param module
	 * @param status
	 */
	private void loadModule(ModuleInfo info, List<ModulePositionInfo> positionInfos) throws Exception {
		StrategyModule strategyModule = moduleFactory.makeModule(info, positionInfos);
		TradeGateway gateway = (TradeGateway) gatewayConnMgr.getGatewayById(info.getAccountGatewayId());
		strategyModule.setCancelOrderHandler(gateway::cancelOrder);
		strategyModule.setSubmitOrderHandler(gateway::submitOrder);
		strategyModule.setDealRecordGenHandler(moduleRepo::saveDealRecord);
		strategyModule.setRunningStateChangeListener(isEnabled -> {
			ModuleInfo moduleInfo = moduleRepo.findModuleInfo(info.getModuleName());
			moduleInfo.setEnabled(isEnabled);
			moduleRepo.saveModuleInfo(moduleInfo);
		});
		strategyModule.setSavingTradeCallback(trade -> {
			moduleRepo.saveTradeRecord(ModuleTradeRecord.builder()
				.contractName(trade.getContract().getFullName())
				.actionTime(trade.getTradeTimestamp())
				.moduleName(info.getModuleName())
				.operation(FieldUtils.chn(trade.getDirection()) + FieldUtils.chn(trade.getOffsetFlag()))
				.price(trade.getPrice())
				.tradingDay(trade.getTradingDay())
				.volume(trade.getVolume())
				.build());
		});

		// 初始化策略数据
		SignalPolicy signalPolicy = strategyModule.getSignalPolicy();
		String unifiedSymbol = signalPolicy.bindedContractSymbol();
		String gatewayId = info.getAccountGatewayId();
		GatewayConnection conn = gatewayConnMgr.getGatewayConnectionById(gatewayId);
		String mktGatewayId = conn.getGwDescription().getBindedMktGatewayId();

		LinkedList<BarField> barList = new LinkedList<>();
		LinkedList<TickField> tickList = new LinkedList<>();
		List<String> availableDates = mdRepo.findDataAvailableDates(mktGatewayId, unifiedSymbol, true);
		for (String date : availableDates.subList(availableDates.size() - info.getNumOfDaysOfDataRef(),
				availableDates.size())) {
			List<MinBarDataPO> dataBarPOList = mdRepo.loadDataByDate(mktGatewayId, unifiedSymbol, date);
			for (MinBarDataPO po : dataBarPOList) {
				barList.add(BarField.parseFrom(po.getBarData()));
				for(byte[] tickData : po.getTicksData()) {
					tickList.add(TickField.parseFrom(tickData));
				}
			}
		}
		signalPolicy.initByTick(tickList);
		signalPolicy.initByBar(barList);
		mdlMgr.addModule(strategyModule);
	}

	/**
	 * 查询所有模组
	 * 
	 * @return
	 */
	public List<ModuleInfo> getCurrentModuleInfos() {
		return moduleRepo.findAllModuleInfo();
	}

	/**
	 * 获取模组实时信息
	 * 
	 * @param moduleName
	 * @return
	 */
	public ModuleRealTimeInfo getModuleRealTimeInfo(String moduleName) {
		ModuleStatus moduleStatus = mdlMgr.getModule(moduleName).getModuleStatus();
		List<ModulePositionInfo> list = moduleStatus.getLogicalPosition().getVolume() == 0 ? Collections.emptyList() : List.of(moduleStatus.getLogicalPosition().convertTo());
		List<ModuleDealRecord> dealRecords = getDealRecords(moduleName);
		List<ModuleDealRecord> subArrOfDealRecords = dealRecords.size() > 30 ? dealRecords.subList(dealRecords.size() - 30, dealRecords.size()) : dealRecords;
		Map<String, ModulePositionInfo> longMap = new HashMap<>();
		Map<String, ModulePositionInfo> shortMap = new HashMap<>();
		list.stream().filter(info -> FieldUtils.isLong(info.getPositionDir())).forEach(info -> longMap.put(info.getUnifiedSymbol(), info));
		list.stream().filter(info -> FieldUtils.isShort(info.getPositionDir())).forEach(info -> shortMap.put(info.getUnifiedSymbol(), info));
		double winningRateOf5Trans = dealRecords.size() < 5 ? -1 : dealRecords.subList(dealRecords.size() - 5, dealRecords.size()).stream().filter(r -> r.getCloseProfit() > 0).count() / 5D;
		double winningRateOf10Trans = dealRecords.size() < 10 ? -1 : dealRecords.subList(dealRecords.size() - 10, dealRecords.size()).stream().filter(r -> r.getCloseProfit() > 0).count() / 10D;
		double meanProfitOf5Trans = dealRecords.size() < 5 ? 0 : dealRecords.subList(dealRecords.size() - 5, dealRecords.size()).stream().mapToDouble(ModuleDealRecord::getCloseProfit).sum() / 5D;
		double meanProfitOf10Trans = dealRecords.size() < 10 ? 0 : dealRecords.subList(dealRecords.size() - 10, dealRecords.size()).stream().mapToDouble(ModuleDealRecord::getCloseProfit).sum() / 10D;
		return ModuleRealTimeInfo.builder()
				.moduleName(moduleName)
				.accountId(mdlMgr.getModule(moduleName).getGateway().getGatewaySetting().getGatewayId())
				.moduleState(moduleStatus.getStateMachine().getCurState())
				.totalPositionProfit(moduleStatus.holdingProfit())
				.avgOccupiedAmount(StatUtils.mean(subArrOfDealRecords.stream().mapToDouble(ModuleDealRecord::getEstimatedOccupiedMoney).toArray()))
				.longPositions(longMap)
				.shortPositions(shortMap)
				.meanProfitOf5Transactions(meanProfitOf5Trans)
				.meanProfitOf10Transactions(meanProfitOf10Trans)
				.winningRateOf5Transactions(winningRateOf5Trans)
				.winningRateOf10Transactions(winningRateOf10Trans)
				.build();
	}

	/**
	 * 获取模组引用数据
	 * 
	 * @param moduleName
	 * @return
	 */
	public List<TimeSeriesData> getModuleDataRef(String moduleName) {
		return mdlMgr.getModule(moduleName).getSignalPolicy().inspectRefData();
	}

	/**
	 * 获取模组交易历史
	 * 
	 * @param moduleName
	 * @return
	 */
	public List<ModuleDealRecord> getDealRecords(String moduleName) {
		return moduleRepo.findDealRecords(moduleName);
	}

	/**
	 * 获取模组成交历史
	 * 
	 * @param moduleName
	 * @return
	 */
	public List<ModuleTradeRecord> getTradeRecords(String moduleName) {
		return moduleRepo.findTradeRecords(moduleName);
	}

	/**
	 * 移除模组
	 * 
	 * @param moduleName
	 */
	public void removeModule(String moduleName) {
		mdlMgr.removeModule(moduleName);
		moduleRepo.deleteModuleInfoById(moduleName);
		moduleRepo.removeModulePosition(moduleName);
		moduleRepo.removeDealRecords(moduleName);
		moduleRepo.removeTradeRecords(moduleName);
	}

	/**
	 * 切换模组状态
	 */
	public boolean toggleState(String moduleName) {
		mdlMgr.getModule(moduleName).toggleRunningState();
		return true;
	}

	/**
	 * 新建模组持仓
	 * 
	 * @param moduleName
	 * @param position
	 * @return
	 */
	public boolean createPosition(String moduleName, ModulePositionInfo position) {
		position.setOpenTime(System.currentTimeMillis());
		position.setOpenTradingDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		return updatePosition(moduleName, position);
	}

	/**
	 * 更新模组持仓
	 * 
	 * @param moduleName
	 * @param position
	 * @return
	 */
	public boolean updatePosition(String moduleName, ModulePositionInfo posInfo) {
		ModuleStatus moduleStatus = mdlMgr.getModule(moduleName).getModuleStatus();
		ContractField contract = contractMgr.getContract(posInfo.getUnifiedSymbol());
		TradeField simTrade = TradeField.newBuilder()
				.setTradeTimestamp(posInfo.getOpenTime())
				.setTradingDay(posInfo.getOpenTradingDay())
				.setPrice(posInfo.getOpenPrice())
				.setVolume(posInfo.getVolume())
				.setContract(contract)
				.setDirection(FieldUtils.isLong(posInfo.getPositionDir()) ? DirectionEnum.D_Buy : DirectionEnum.D_Sell)
				.build();
		moduleStatus.updatePosition(simTrade);
		moduleStatus.getLogicalPosition().stopLoss(posInfo.getStopLossPrice());
		List<ModulePositionInfo> posList = List.of(moduleStatus.getLogicalPosition().convertTo());
		moduleRepo.saveTradeRecord(ModuleTradeRecord.builder()
				.moduleName(moduleName)
				.contractName(contract.getFullName())
				.actionTime(posInfo.getOpenTime())
				.price(posInfo.getOpenPrice())
				.volume(posInfo.getVolume())
				.tradingDay(posInfo.getOpenTradingDay())
				.operation(FieldUtils.chn(simTrade.getDirection()) + FieldUtils.chn(simTrade.getOffsetFlag()))
				.build());
		moduleRepo.saveModulePosition(ModulePositionPO.builder()
				.moduleName(moduleName)
				.positions(posList)
				.build());
		return true;
	}

	/**
	 * 移除模组持仓
	 * 
	 * @param moduleName
	 * @param unifiedSymbol
	 * @param dir
	 * @return
	 */
	public boolean removePosition(String moduleName) {
		ModuleStatus moduleStatus = mdlMgr.getModule(moduleName).getModuleStatus();
		moduleStatus.removePosition();
		moduleRepo.removeModulePosition(moduleName);
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		CompletableFuture.runAsync(() -> {
			try {
				List<ModuleInfo> modules = getCurrentModuleInfos();
				log.debug("开始加载模组信息: {}条", modules.size());
				for (ModuleInfo m : modules) {
					ModulePositionPO po = moduleRepo.loadModulePosition(m.getModuleName());
					List<ModulePositionInfo> list = po == null ? Collections.emptyList() : po.getPositions();
					loadModule(m, list);
				}
			} catch (Exception e) {
				log.error("模组加载异常", e);
			}
		}, CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS));
	}

}
