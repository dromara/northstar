package org.dromara.northstar.module;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.exception.NoSuchContractException;
import org.dromara.northstar.common.model.ContractChartDescription;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.IndicatorVO;
import org.dromara.northstar.common.model.ModuleChartDescription;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleIndicatorDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.common.model.TimeValues;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.ModuleLoggingFactory;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.contract.OptionChainContract;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.IndicatorValueUpdateHelper;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.kline.BarTimePadding;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.MergedBarListener;
import org.dromara.northstar.strategy.OrderRequestFilter;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.support.utils.bar.BarMergerRegistry;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class ModuleContext implements IModuleContext{
	
	private final ModuleDescription moduleDescription;
	
	private final Logger logger;
	
	protected TradeStrategy tradeStrategy;
	
	protected IModuleRepository moduleRepo;
	
	protected ModuleAccount moduleAccount;
	
	/* originOrderId -> orderReq */
	protected ConcurrentMap<String, SubmitOrderReq> orderReqMap = new ConcurrentHashMap<>();
	
	/* unifiedSymbol -> contract */
	protected ConcurrentMap<String, Contract> contractMap = new ConcurrentHashMap<>();
	
	protected Table<Contract, Interval, Long> barFilterMap = HashBasedTable.create();
	
	/* contract -> indicatorName -> indicator */
	protected Table<Contract, String, Indicator> indicatorNameTbl = HashBasedTable.create();
	
	protected Set<IndicatorValueUpdateHelper> indicatorHelperSet = new HashSet<>();
	
	protected final BarMergerRegistry registry;

	protected final ChartBuffer chartBuffer;
	
	protected boolean enabled;
	
	protected LocalDate tradingDay;
	
	protected IMarketCenter mktCenter;
	
	protected OrderRequestFilter orderReqFilter;
	
	protected ModuleStateMachine moduleStateMachine;
	
	private Map<Contract, BarTimePadding> barTimePaddingMap = new HashMap<>();
	
	public ModuleContext(TradeStrategy tradeStrategy, ModuleDescription moduleDescription, ModuleAccount moduleAcc, IContractManager contractMgr, 
			IModuleRepository moduleRepo) {
		this.moduleDescription = moduleDescription;
		this.tradeStrategy = tradeStrategy;
		this.moduleRepo = moduleRepo;
		this.mktCenter = (IMarketCenter) contractMgr;
		this.registry = new BarMergerRegistry();
		this.logger = getLogger(getClass());
		this.chartBuffer = new ChartBuffer(moduleDescription.getModuleCacheDataSize());
		this.moduleStateMachine = new ModuleStateMachine();
		this.orderReqFilter = new DefaultOrderFilter(moduleDescription.getBindedContracts(), this);
		
		moduleDescription.getBindedContracts()
			.forEach(csi -> {
				IContract contract = contractMgr.getContract(Identifier.of(csi.getId()));
				Contract c = contract.contract();
				Interval barInterval = Interval.fromAlias(moduleDescription.getKlineInterval());
				barTimePaddingMap.put(c, new BarTimePadding(barInterval, c));
				if(contract instanceof OptionChainContract) {
					contractMap.put(c.unifiedSymbol(), c);
					contract.memberContracts().forEach(mc -> initForContract(mc.contract(), barInterval));
				} else {
					initForContract(contract.contract(), barInterval);
				}
			});
		
		this.moduleAccount = moduleAcc;
		
		String name = moduleDescription.getName();
		ModuleLoggingFactory.register(name, moduleStateMachine);
		ModuleLoggingFactory.register(name, moduleAcc);
		
		moduleStateMachine.setModuleAccount(moduleAccount);
	}
	
	private void initForContract(Contract cf, Interval interval) {
		contractMap.put(cf.unifiedSymbol(), cf);
		registry.addListener(cf, interval, tradeStrategy);
		registry.addListener(cf, interval, this);
	}

	@Override
	public Contract getContract(String unifiedSymbol) {
		Assert.hasText(unifiedSymbol, "合约编码不能为空");
		if(!contractMap.containsKey(unifiedSymbol)) {
			throw new NoSuchContractException(unifiedSymbol);
		}
		return contractMap.get(unifiedSymbol);
	}

	@Override
	public void submitOrderReq(SubmitOrderReq orderReq) {
		if(!isEnabled()) {
			logger.info("策略处于停用状态，忽略委托单");
			return;
		}

		logger.info("准备发单：{}，{} {} {}", orderReq.contract().name(), orderReq.originOrderId(), orderReq.direction(), orderReq.offsetFlag());
		try {
			if(Objects.nonNull(orderReqFilter)) {
				orderReqFilter.doFilter(orderReq);
			}
			moduleAccount.submitOrder(orderReq);
			moduleStateMachine.onSubmitReq();
			orderReqMap.put(orderReq.originOrderId(), orderReq);
		} catch (Exception e) {
			logger.error("发单失败。原因：{}", e.getMessage());
			moduleStateMachine.onFailSubmitReq();
			logger.warn("发单异常，主动停用模组");
			setEnabled(false);
		}
	}
	
	@Override
	public void cancelOrder(CancelOrderReq cancelReq) {
		if(!orderReqMap.containsKey(cancelReq.originOrderId())) {
			logger.warn("找不到订单：{}", cancelReq.originOrderId());
			return;
		}
		if(!getState().isOrdering()) {
			logger.info("非下单状态，忽略撤单请求：{}", cancelReq.originOrderId());
			return;
		}
		logger.info("撤单：{}", cancelReq.originOrderId());
		if(moduleAccount.cancelOrder(cancelReq)) {
			orderReqMap.remove(cancelReq.originOrderId());
		}
	}

	@Override
	public Interval moduleBarInterval() {
		return Interval.fromAlias(moduleDescription.getKlineInterval());
	}

	@Override
	public IAccount getAccount(Contract contract) {
		return moduleAccount.getContractAccountMap().get(contract);
	}

	@Override
	public IAccount getModuleAccount() {
		return moduleAccount;
	}

	@Override
	public ModuleState getState() {
		return moduleStateMachine.getState();
	}

	@Override
	public void disabledModule() {
		logger.warn("策略层主动停用模组");
		setEnabled(false);
	}

	@Override
	public Logger getLogger(Class<?> clz) {
		return ModuleLoggingFactory.getLogger(moduleDescription.getName(), clz.getName());
	}
	
	@Override
	public void registerIndicator(Indicator indicator) {
		checkIndicator(indicator);
		Configuration cfg = indicator.getConfiguration();
		IndicatorValueUpdateHelper helper = new IndicatorValueUpdateHelper(indicator);
		indicatorHelperSet.add(helper);
		registry.addListener(cfg.contract(), cfg.interval(), helper);
		// 递归子指标
		for(Indicator in : indicator.dependencies()) {
			registerIndicator(in);
		}
	}
	
	public void checkIndicator(Indicator indicator) {
		Configuration cfg = indicator.getConfiguration();
		String indicatorName = cfg.indicatorID();
		logger.trace("检查指标配置信息：{}", indicatorName);
		Assert.isTrue(cfg.cacheLength() > 0, "指标回溯长度必须大于0，当前为：" + cfg.cacheLength());
		if(Boolean.TRUE.equals(cfg.visible())) {		// 不显示的指标可以不做重名校验
			Assert.isTrue(!indicatorNameTbl.contains(cfg.contract(), indicatorName) || indicator.equals(indicatorNameTbl.get(cfg.contract(), indicatorName)), 
					() -> String.format("指标 [%s -> %s] 已存在。不能重名", cfg.contract().unifiedSymbol(), indicatorName));
			indicatorNameTbl.put(cfg.contract(), indicatorName, indicator);
		}
	}
	
	@Override
	public void onTick(Tick tick) {
		logger.trace("TICK数据: {} {} {} {}，最新价：{}，累计成交：{}，成交量：{}，累计持仓：{}，持仓量：{}", 
				tick.contract().unifiedSymbol(), tick.actionDay(), tick.actionTime(), tick.actionTimestamp(),
				tick.lastPrice(), tick.volume(), tick.volumeDelta(), tick.openInterest(), tick.openInterestDelta());
		if(!Objects.equals(tick.tradingDay(), tradingDay)) {
			tradingDay = tick.tradingDay();
		}
		
		tradeStrategy.onTick(tick);
	}

	@Override
	public void onBar(Bar bar) {
		logger.trace("Bar数据: {} {} {} {} {} unstable:{}", bar.contract().name(), bar.interval(), bar.actionDay(), bar.actionTime(), bar.actionTimestamp(), bar.unstable());
		if(barFilterMap.contains(bar.contract(), bar.interval()) && barFilterMap.get(bar.contract(), bar.interval()) > bar.actionTimestamp()) {
			//过滤掉可能存在的重复数据
			logger.trace("Bar数据被过滤：当前 {} 最新K线的时间为 {}", bar.interval(), CommonUtils.millsToLocalDateTime(barFilterMap.get(bar.contract(), bar.interval())));
			return;
		}
		barFilterMap.put(bar.contract(), bar.interval(), bar.actionTimestamp());
		registry.onBar(bar);		
	}
	
	@Override
	public void onMergedBar(Bar bar) {
		logger.debug("合并Bar信息: {} {} {} {}，最新价: {}，成交量：{}，累计持仓：{}，持仓量：{}", bar.contract().unifiedSymbol(), bar.actionDay(), bar.actionTime(), bar.actionTimestamp(), 
				bar.closePrice(), bar.volumeDelta(), bar.openInterest(), bar.openInterestDelta());
		// 由于模组图表可容忍延迟更新，仅对已确定的K线进行更新
		chartBuffer.update(bar, indicatorHelperSet);
		
		if(isEnabled() && bar.channelType() != ChannelType.PLAYBACK) {
			moduleRepo.saveRuntime(getRuntimeDescription());
		}
	}
	
	@Override
	public void onOrder(Order order) {
		if(!orderReqMap.containsKey(order.originOrderId())) {
			return;
		}
		logger.info("收到订单反馈：{} 合约：{} 订单状态：{} {}", order.originOrderId(), order.contract().name(), order.orderStatus(), order.statusMsg());
		if(!OrderUtils.isValidOrder(order) || OrderUtils.isDoneOrder(order)) {
			// 延时3秒再移除订单信息，避免移除了订单信息后，成交无法匹配的问题
			CompletableFuture.runAsync(() -> orderReqMap.remove(order.originOrderId()), CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS));
		}
		moduleAccount.onOrder(order);
		moduleStateMachine.onOrder(order);
		tradeStrategy.onOrder(order);
	}

	@Override
	public void onTrade(Trade trade) {
		if(!orderReqMap.containsKey(trade.originOrderId()) && !StringUtils.equals(trade.originOrderId(), Constants.MOCK_ORDER_ID)) {
			return;
		} 
		if(logger.isInfoEnabled()) {
			logger.info("收到成交反馈：{}， 操作：{}{}，合约：{}，价格：{}， 交易量：{}", trade.originOrderId(), FieldUtils.chn(trade.direction()),
					FieldUtils.chn(trade.offsetFlag()), trade.contract().name(), trade.price(), trade.volume());
		}
		moduleAccount.onTrade(trade);
		moduleStateMachine.onTrade(trade);
		tradeStrategy.onTrade(trade);
		moduleRepo.saveRuntime(getRuntimeDescription());
	}

	@Override
	public ModuleRuntimeDescription getRuntimeDescription() {
		return ModuleRuntimeDescription.builder()
				.moduleName(moduleDescription.getName())
				.storeObject(tradeStrategy.getStoreObject())
				.build();
	}

	private static final List<String> DEFAULT_FIELD_NAMES = List.of("open", "high", "low", "close", "volume", "openInterest", "openInterestDelta", "timestamp");

	@Override
	public ModuleChartDescription getChartDescription() {
		List<ContractChartDescription> mergeData = new ArrayList<>();
		List<ModuleIndicatorDescription> allIndicators = new ArrayList<>();
		List<ModuleIndicatorDescription> customIndicators = tradeStrategy.customIndicators();
		validateCustomIndicators(customIndicators);
		allIndicators.addAll(chartBuffer.getIndicatorDescriptions());
		allIndicators.addAll(customIndicators);

		chartBuffer.getBarBuffers().entrySet().forEach(e -> {
			Contract c = e.getKey();
			BarTimePadding padding = barTimePaddingMap.get(c);
			List<Bar> bars = e.getValue();
			
			List<String> fieldNames = new ArrayList<>(DEFAULT_FIELD_NAMES);
			Map<Long, List<Double>> fieldValues = new LinkedHashMap<>();
			
			List<ModuleIndicatorDescription> moduleIndicators = allIndicators.stream()
					.filter(ind -> c.contractId().equals(ind.getContract().getId()))
					.toList();
			List<IndicatorVO> indicators = moduleIndicators.stream().map(ind -> IndicatorVO.builder()
					.name(ind.getName())
					.style(ind.getStyle())
					.build())
					.toList();
			fieldNames.addAll(moduleIndicators.stream().map(ModuleIndicatorDescription::getName).toList());
			
			Table<Long, String, Double> indicatorValueTable = HashBasedTable.create();
			for(ModuleIndicatorDescription ind : moduleIndicators) {
				ind.getValues().stream()
					.filter(tv -> tv.getTimestamp() > 0)
					.forEach(tv -> {
						if(c.usingStartTimeInBarTime()) {
							indicatorValueTable.put(CommonUtils.localDateTimeToMills(padding.openTimeInMinute(tv.getTimestamp())), ind.getName(), tv.getValue());
						} else {
							indicatorValueTable.put(CommonUtils.localDateTimeToMills(padding.cutoffTimeInMinute(tv.getTimestamp())), ind.getName(), tv.getValue());
						}
					});
			}
			bars.forEach(bar -> {
				List<Double> values = new LinkedList<>();
				values.add(bar.openPrice());
				values.add(bar.highPrice());
				values.add(bar.lowPrice());
				values.add(bar.closePrice());
				values.add(bar.volumeDelta());
				values.add((double)bar.openInterest());
				values.add((double)bar.openInterestDelta());
				values.add((double)bar.actionTimestamp());
				for(ModuleIndicatorDescription ind : moduleIndicators) {
					Double v = indicatorValueTable.get(bar.actionTimestamp(), ind.getName());
					if(Objects.isNull(v)) {
						v = Double.NaN;
					}
					values.add(v);
				}
				fieldValues.put(bar.actionTimestamp(), values);
			});
			
			List<TimeValues> tvs = fieldValues.entrySet().stream()
					.map(en -> TimeValues.builder()
							.timestamp(en.getKey())
							.values(en.getValue().toArray(Double[]::new))
							.build())
					.toList();
			
			ContractChartDescription ccd = ContractChartDescription.builder()
					.contract(ContractSimpleInfo.from(c))
					.fields(fieldNames.toArray(String[]::new))
					.indicators(indicators)
					.timedValues(tvs)
					.build();
			
			mergeData.add(ccd);
		});
		
		return ModuleChartDescription.builder()
				.data(mergeData)
				.strategyInfos(tradeStrategy.strategyInfos())
				.build();
	}
	
	private void validateCustomIndicators(List<ModuleIndicatorDescription> indicators) {
		Map<ContractSimpleInfo, Set<String>> contractIndicatorNameSet = new HashMap<>();
		for(ModuleIndicatorDescription indicator : indicators) {
			if(!contractIndicatorNameSet.containsKey(indicator.getContract())) {
				contractIndicatorNameSet.put(indicator.getContract(), new HashSet<>());
			}
			if(contractIndicatorNameSet.get(indicator.getContract()).contains(indicator.getName())) {
				throw new IllegalStateException("自定义图表指标名称重复：" + indicator.getName());
			}
			contractIndicatorNameSet.get(indicator.getContract()).add(indicator.getName());
			Set<Long> timeSet = new HashSet<>();
			for(TimeSeriesValue tv : indicator.getValues()) {
				if(timeSet.contains(tv.getTimestamp())) {
					throw new IllegalStateException(String.format("合约%s指标%s数据有重复，时间：%s", indicator.getContract().getName(), indicator.getName(), CommonUtils.millsToLocalDateTime(tv.getTimestamp())));
				}
				timeSet.add(tv.getTimestamp());
			}
		}
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		logger.info("【{}】 模组", enabled ? "启用" : "停用");
		this.enabled = enabled;
		moduleRepo.saveRuntime(getRuntimeDescription());
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setOrderRequestFilter(OrderRequestFilter filter) {
		this.orderReqFilter = filter;
	}

	@Override
	public int getDefaultVolume() {
		return moduleDescription.getDefaultVolume();
	}

	@Override
	public TradeStrategy getStrategy() {
		return tradeStrategy;
	}

	@Override
	public void addMergedBarListener(Contract contract, Interval interval, MergedBarListener listener) {
		registry.addListener(contract, interval, listener);
	}

	@Override
	public List<Contract> bindedContracts() {
		return moduleDescription.getBindedContracts()
				.stream()
				.map(csi -> getContract(csi.getUnifiedSymbol()))
				.toList();
	}

	@Override
	public ModuleDescription getModuleDescription() {
		return moduleDescription;
	}

}
