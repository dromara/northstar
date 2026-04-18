package org.dromara.northstar.module;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.ExplorerTaskDescription;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleAccountRuntimeDescription;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.StrategyMetaInfo;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.data.jdbc.ModuleDealRecordRepository;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.gateway.mktdata.NorthstarDataSource;
import org.dromara.northstar.strategy.DynamicParamsAware;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.TradeStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSONObject;

import lombok.Setter;

/**
 * 负责根据回测任务生成回测执行器
 */
@Setter
@Component
public class BacktestModuleFactory {
	
	public static final int BUF_SIZE = 500;

	@Autowired
	protected NorthstarDataSource dataSource;
	
	@Autowired
	protected MarketCenter mktCenter;
	
	@Autowired
	protected AccountManager accMgr;
	
	@Autowired
	protected IModuleRepository moduleRepo;
	
	@Autowired
	protected ModuleDealRecordRepository dealRecordRepo;
	
	public BacktestRunner createTestRunner(ExplorerTaskDescription task) {
		Map<Contract, List<IModule>> contractModulesMap = new HashMap<>();
		List<IModule> moduleList = new ArrayList<>();
		
		StrategyMetaInfo strategyComponent = task.getStrategySetting();
		LocalDate preloadDate = LocalDate.parse(task.getPreloadFrom(), DateTimeConstant.D_FORMAT_FORMATTER);
		LocalDate startDate = LocalDate.parse(task.getStartDate(), DateTimeConstant.D_FORMAT_FORMATTER);
		LocalDate endDate = LocalDate.parse(task.getEndDate(), DateTimeConstant.D_FORMAT_FORMATTER);
		task.getContracts().forEach(c -> {
			task.getIntervals().forEach(interval -> {
				Interval barInterval = Interval.fromAlias(interval);
				String moduleName = moduleName(task.getTaskName(), c, barInterval);
				Contract contract = mktCenter.getContract(Identifier.of(c.getId())).contract();
				List<Bar> preloadBars = dataSource.getDailyData(contract, startDate.minusMonths(1), startDate);
				double avgPrice = preloadBars.stream().mapToDouble(Bar::closePrice).average().getAsDouble();
				double costPerShare = avgPrice * contract.multiplier() * contract.contractDefinition().marginRatio();	// 每手成本
				// 在初始资金的一半决定默认仓位
				int defaultVol = Math.max(1, (int) (task.getInitBalance() / (2 * costPerShare)));
				ModuleDescription md = ModuleDescription.builder()
						.name(moduleName)
						.initBalance(task.getInitBalance())
						.type(ModuleType.SPECULATION)
						.usage(ModuleUsage.PLAYBACK)
						.closingPolicy(ClosingPolicy.FIRST_IN_FIRST_OUT)
						.bindedAccounts(Collections.emptyList())
						.bindedContracts(List.of(c))
						.moduleCacheDataSize(BUF_SIZE)
						.klineInterval(barInterval.getAlias())
						.defaultVolume(defaultVol)
						.build();
				ModuleRuntimeDescription mrd = ModuleRuntimeDescription.builder()
						.moduleName(md.getName())
						.storeObject(new JSONObject())
						.moduleAccountRuntime(ModuleAccountRuntimeDescription.builder()
								.initBalance(md.getInitBalance())
								.build())
						.build();
				TradeStrategy strategy;
				try {
					strategy = resolveComponent(strategyComponent);
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
				ModuleContext moduleCtx = new PlaybackModuleContext(strategy, md, mrd, mktCenter, moduleRepo);
				strategy.setContext(moduleCtx);
				strategy.setStoreObject(mrd.getStoreObject());
				IModule module = new BacktestModule(moduleCtx);
				
				if(!contractModulesMap.containsKey(contract)) {
					contractModulesMap.put(contract, new ArrayList<IModule>());
				}
				contractModulesMap.get(contract).add(module);
				moduleList.add(module);
			});
		});
		
		return BacktestRunner.builder()
				.taskDescription(task)
				.dataSource(dataSource)
				.preloadDate(preloadDate)
				.fromDate(startDate)
				.toDate(endDate)
				.moduleList(moduleList)
				.contractModulesMap(contractModulesMap)
				.dealRecordRepo(dealRecordRepo)
				.build();
	}
	
	private String moduleName(String taskName, ContractSimpleInfo c, Interval barInterval) {
		return String.format("Test_%s_%s_%s", taskName, c.getName(), barInterval.getAlias());
	}
	
	@SuppressWarnings("unchecked")
	private <T extends DynamicParamsAware> T resolveComponent(StrategyMetaInfo metaInfo) throws Exception {
		String clzName = metaInfo.getClassName();
		String paramClzName = clzName + "$InitParams";
		Class<?> type = Class.forName(clzName);
		Class<?> paramType = Class.forName(paramClzName);
		DynamicParamsAware obj = (DynamicParamsAware) type.getDeclaredConstructor().newInstance();
		DynamicParams paramObj = (DynamicParams) paramType.getDeclaredConstructor().newInstance();
		paramObj.resolveFromSource(metaInfo.getSettings());
		obj.initWithParams(paramObj);
		return (T) obj;
	}
}
