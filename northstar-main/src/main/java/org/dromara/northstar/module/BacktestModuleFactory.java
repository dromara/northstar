package org.dromara.northstar.module;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.ExplorerTaskDescription;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.StrategyMetaInfo;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.data.IAccountRepository;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.gateway.mktdata.NorthstarDataSource;
import org.dromara.northstar.gateway.sim.trade.AccountPosition;
import org.dromara.northstar.strategy.DynamicParamsAware;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.TradeStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import lombok.Setter;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

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
	protected IAccountRepository accountRepo;
	
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
				List<Bar> preloadBars = dataSource.getData(contract, Interval.D1, startDate.minusMonths(1), startDate);
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
						.defaultVolume(defaultVol)
						.build();
				ModuleRuntimeDescription mrd = ModuleRuntimeDescription.builder()
						.moduleName(md.getName())
						.storeObject(new JSONObject())
						.build();
				
				Table<Contract, PositionDirectionEnum, AccountPosition> positionTbl = HashBasedTable.create();
				positionTbl.put(contract, PositionDirectionEnum.PD_Long, new AccountPosition(moduleName, contract, PositionDirectionEnum.PD_Long, accountRepo));
				positionTbl.put(contract, PositionDirectionEnum.PD_Short, new AccountPosition(moduleName, contract, PositionDirectionEnum.PD_Short, accountRepo));
				
				ModuleAccount moduleAccount = new ModuleAccount(md.getName(), md.getInitBalance(), 0, 0, 
						positionTbl, Map.of(contract, new DummyAccount()));
				TradeStrategy strategy;
				try {
					strategy = resolveComponent(strategyComponent);
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
				ModuleContext moduleCtx = new PlaybackModuleContext(strategy, md, moduleAccount, mktCenter, moduleRepo);
				strategy.setContext(moduleCtx);
				strategy.setStoreObject(mrd.getStoreObject());
				IModule module = new BacktestModule(moduleCtx);
				
				if(!contractModulesMap.containsKey(contract)) {
					contractModulesMap.put(contract, new ArrayList<>());
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
				.accountRepo(accountRepo)
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
	
	private static class DummyAccount implements IAccount{

		@Override
		public void onOrder(Order order) {
		}

		@Override
		public void onTrade(Trade trade) {
		}

		@Override
		public void submitOrder(SubmitOrderReq orderReq) throws TradeException {
		}

		@Override
		public boolean cancelOrder(CancelOrderReq cancelReq) {
			return false;
		}

		@Override
		public double accountBalance() {
			return 0;
		}

		@Override
		public double availableBalance() {
			return 0;
		}

		@Override
		public String getId() {
			return "BacktestDummyAccount";
		}

		@Override
		public Optional<Position> getPosition(PositionDirectionEnum posDirection, Contract contract) {
			return Optional.empty();
		}

		@Override
		public List<Order> getPendingOrders() {
			return Collections.emptyList();
		}

		@Override
		public TradeGateway getTradeGateway() {
			throw new UnsupportedOperationException();
		}
		
	}
}
