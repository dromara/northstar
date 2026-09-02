package org.dromara.northstar.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.ContractChartDescription;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.IndicatorVO;
import org.dromara.northstar.common.model.ModuleIndicatorDescription;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.common.model.TimeValues;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.IndicatorValueUpdateHelper;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.dromara.northstar.indicator.model.RingArray;
import org.dromara.northstar.kline.BarTimePadding;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.MergedBarListener;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * 管理同一个周期的数据更新与分发
 */
public class TimeFrameScopedRegistry {
	
	private final Logger logger;

	private final Interval timeFrame;
	
	private final IModuleContext context;
	
	private final Map<Contract, BarTimePadding> barTimePaddingMap;
	private final Map<Contract, RingArray<Bar>> barBuffMap;

    private final Map<Indicator, RingArray<Num>> indicatorBufMap = new HashMap<>();
    
    private final Map<Contract, List<IndicatorValueUpdateHelper>> indicatorHelperMap = new HashMap<>();
    
    private final Set<String> indicatorNameSet = new HashSet<>();
    
    private final Map<Contract, Long> barFilterMap = new HashMap<>();
    
    private final Map<Contract, MergedBarListener> listenerMap = new HashMap<>();
	
	public TimeFrameScopedRegistry(Interval timeFrame, IModuleContext context) {
		this.timeFrame = timeFrame;
		this.context = context;
		this.logger = context.getLogger(getClass());
		this.barBuffMap = context.bindedContracts()
			.stream()
			.collect(Collectors.toMap(c -> c, _ -> new RingArray<>(context.getModuleDescription().getModuleCacheDataSize())));
		this.barTimePaddingMap = context.bindedContracts()
				.stream()
				.collect(Collectors.toMap(c -> c, c -> new BarTimePadding(timeFrame, c)));
	}
	
	public void registerIndicator(Indicator indicator) {
		doRegister(indicator, true);
	}
	
	private void doRegister(Indicator indicator, boolean isRoot) {
		if(indicatorBufMap.containsKey(indicator)) {
			// 同一指标实例重复注册直接忽略，保证幂等
			return;
		}
		checkIndicator(indicator);
		indicatorBufMap.put(indicator, new RingArray<Num>(context.getModuleDescription().getModuleCacheDataSize()));
		if(isRoot) {
			// 更新助手自身会递归更新依赖指标，因此只需为根指标注册，
			// 且同一合约可能注册多个指标，需要用列表保存避免相互覆盖
			Configuration cfg = indicator.getConfiguration();
			indicatorHelperMap.computeIfAbsent(cfg.contract(), _ -> new ArrayList<>()).add(new IndicatorValueUpdateHelper(indicator));
		}
		// 递归子指标
		for(Indicator in : indicator.dependencies()) {
			doRegister(in, false);
		}
	}
	
	private void checkIndicator(Indicator indicator) {
		Configuration cfg = indicator.getConfiguration();
		String indicatorName = cfg.indicatorName();
		logger.trace("检查指标配置信息：{} {}", indicatorName, cfg.interval().getAlias());
		Assert.isTrue(cfg.cacheLength() > 0, "指标回溯长度必须大于0，当前为：" + cfg.cacheLength());
		if(Boolean.TRUE.equals(cfg.visible())) {		// 不显示的指标可以不做重名校验
			Assert.isTrue(!indicatorNameSet.contains(indicatorName), 
					() -> String.format("指标 [%s -> %s] 已存在。不能重名", cfg.contract().unifiedSymbol(), indicatorName));
			indicatorNameSet.add(indicatorName);
		}
	}
	
	public synchronized void registerBarListener(Contract contract, MergedBarListener listener) {
		listenerMap.put(contract, listener);
	}
	
	public synchronized void onBar(Bar bar) {
		if(bar.interval() != timeFrame) {
			return;
		}
		// 过滤掉可能存在的重复数据
		if(barFilterMap.containsKey(bar.contract()) && barFilterMap.get(bar.contract()) > bar.actionTimestamp()) {
			logger.trace("Bar数据被过滤：当前 {} 最新K线的时间为 {}", bar.interval(), CommonUtils.millsToLocalDateTime(barFilterMap.get(bar.contract())));
			return;
		}
		barFilterMap.put(bar.contract(), bar.actionTimestamp());
		
		// 更新指标计算
		if(indicatorHelperMap.containsKey(bar.contract())) {
			indicatorHelperMap.get(bar.contract()).forEach(helper -> helper.onMergedBar(bar));
		}
		indicatorBufMap.entrySet().stream()
			.filter(e -> e.getKey().getConfiguration().contract().equals(bar.contract()))
			.filter(e -> e.getKey().getConfiguration().visible())
			.forEach(e -> {
				Indicator indicator = e.getKey();
				RingArray<Num> buffer = e.getValue();
				if(indicator.isReady() && (indicator.getConfiguration().ifPlotPerBar() || !indicator.get(0).unstable())) {
					buffer.update(indicator.get(0), bar.unstable());
	            } else {
	            		buffer.update(Num.NaN(), false);
	            }
			});
		
		// 跳过没确定BAR数据
		if(bar.unstable()) {
			return;
		}
		
		// 记录数据缓存
		if(barBuffMap.containsKey(bar.contract())) {
			barBuffMap.get(bar.contract()).update(bar, false);
		}
		
		// 分发handler
		Optional.ofNullable(listenerMap.get(bar.contract())).ifPresent(listener -> listener.onMergedBar(bar));
		
		// 分发strategy
		context.getStrategy().onMergedBar(bar);
	}
	
	private static final List<String> DEFAULT_FIELD_NAMES = List.of("open", "high", "low", "close", "volume", "openInterest", "openInterestDelta", "timestamp");
	
	public List<ContractChartDescription> visulize() {
		List<ModuleIndicatorDescription> customIndicators = context.getStrategy().customIndicators();
		validateCustomIndicators(customIndicators);
		return context.bindedContracts()
				.stream()
				.map(contract -> {
					BarTimePadding padding = barTimePaddingMap.get(contract);
					List<Bar> bars = barBuffMap.get(contract).toList();
					
					List<String> fieldNames = new ArrayList<>(DEFAULT_FIELD_NAMES);
					Map<Long, List<Double>> fieldValues = new LinkedHashMap<>();
					
					List<ModuleIndicatorDescription> allIndicators = new ArrayList<>();
					List<ModuleIndicatorDescription> strategyCustomIndicators = customIndicators.stream()
							.filter(mid -> mid.getTimeFrame() == null || mid.getTimeFrame() == timeFrame)
							.toList();
					List<ModuleIndicatorDescription> scopedIndicators = indicatorBufMap.entrySet()
							.stream()
							.filter(e -> e.getKey().getConfiguration().contract().equals(contract))
							.map(e -> ModuleIndicatorDescription.builder()
									.name(e.getKey().getConfiguration().indicatorName())
									.contract(ContractSimpleInfo.from(e.getKey().getConfiguration().contract()))
									.style(e.getKey().getConfiguration().lineStyle())
									.values(e.getValue().toList().stream().map(n -> new TimeSeriesValue(n.value(), n.timestamp())).toList())
									.build())
							.toList();
					allIndicators.addAll(strategyCustomIndicators);
					allIndicators.addAll(scopedIndicators);
					
					List<IndicatorVO> indicators = allIndicators.stream()
							.map(ind -> IndicatorVO.builder()
								.name(ind.getName())
								.style(ind.getStyle())
								.build())
							.toList();
					fieldNames.addAll(allIndicators.stream().map(ModuleIndicatorDescription::getName).toList());
					
					Table<Long, String, Double> indicatorValueTable = HashBasedTable.create();
					for(ModuleIndicatorDescription ind : allIndicators) {
						ind.getValues().stream()
							.filter(tv -> tv.getTimestamp() > 0)
							.forEach(tv -> {
								if(contract.usingStartTimeInBarTime()) {
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
						for(ModuleIndicatorDescription ind : allIndicators) {
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
					
					return ContractChartDescription.builder()
							.contract(ContractSimpleInfo.from(contract))
							.fields(fieldNames.toArray(String[]::new))
							.indicators(indicators)
							.timedValues(tvs)
							.build();
				})
				.toList();
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
					throw new IllegalStateException(String.format("合约【%s】指标【%s】数据有重复，时间：%s", indicator.getContract().getName(), indicator.getName(), CommonUtils.millsToLocalDateTime(tv.getTimestamp())));
				}
				timeSet.add(tv.getTimestamp());
			}
		}
	}

}
