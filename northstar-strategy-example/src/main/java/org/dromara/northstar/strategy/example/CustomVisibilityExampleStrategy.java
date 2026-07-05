package org.dromara.northstar.strategy.example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.constant.LineStyle;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.ListValue;
import org.dromara.northstar.common.model.ModuleIndicatorDescription;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.common.model.StringValue;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.model.RingArray;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.springframework.util.Assert;

/**
 * 该示例仅用于展示如何把策略中的一些信息在前端监控台的模组图表中可视化
 * 该示例主要展示两个用法：
 * 一个是自计算（不依赖指标框架）的ZigZag指标
 * 另一个是计算最近一段时间里的交易区间（类似股票软件中的筹码分布图，但不是用图形表示，而是用文字描述统计值来表示）
 * @since v8.0
 */

@StrategicComponent(CustomVisibilityExampleStrategy.NAME)
public class CustomVisibilityExampleStrategy extends AbstractStrategy implements TradeStrategy{

	public static final String NAME = "示例-策略信息可视化";
	
	private InitParams params;
	
	private RingArray<Bar> barBuff;
	
	private Contract c;
	
	@Override
	protected void initIndicators() {
		Assert.isTrue(ctx.bindedContracts().size() == 1, "该示例只能绑定一个合约");
		c = ctx.bindedContracts().get(0);
		barBuff = new RingArray<>(params.refSize);
	}

	@Override
	public void onMergedBar(Bar bar) {
		barBuff.update(bar, bar.unstable());
	}
	
	/*******************************************************************************************************************************/
	/**
	 * 该方法用于展示策略信息
	 */
	@Override
	public List<ListValue> strategyInfos() {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		barBuff.toList().stream()
			.map(this::sampling)
			.flatMap(map -> map.entrySet().stream())
			.forEach(e -> {
				int ticks = e.getKey();
				int volumes = e.getValue();
				for(int i=0; i<volumes; i++) {
					stats.addValue(ticks);
				}
			});
		
		StringValue sect95 = StringValue.builder().name("95分位").value(toPrice(c, (int)stats.getPercentile(95)).toString()).build();
		StringValue sect75 = StringValue.builder().name("75分位").value(toPrice(c, (int)stats.getPercentile(75)).toString()).build();
		StringValue sect50 = StringValue.builder().name("中位数").value(toPrice(c, (int)stats.getPercentile(50)).toString()).build();
		StringValue sect25 = StringValue.builder().name("25分位").value(toPrice(c, (int)stats.getPercentile(25)).toString()).build();
		StringValue sect5 = StringValue.builder().name("5分位").value(toPrice(c, (int)stats.getPercentile(5)).toString()).build();
		
		ListValue distribution = ListValue.builder()
				.name("近期筹码分布区间")
				.value(List.of(sect95, sect75, sect50, sect25, sect5))
				.build();
		return List.of(distribution);
	}
	
	/* 把bar数据转换写 价位->成交量分布 */
	private Map<Integer, Integer> sampling(Bar bar) {
		int ticksOnHigh = (int) (bar.highPrice() / c.priceTick());
		int ticksOnLow = (int) (bar.lowPrice() / c.priceTick());
		int buckets = ticksOnHigh - ticksOnLow + 1; //把价位分N份，用来平均分配成交量（粗略计算）
		int volumePerBucket = (int) (bar.volumeDelta() / buckets);
		Map<Integer, Integer> resultMap = new HashMap<>();
		for(int t=ticksOnLow; t<=ticksOnHigh; t++) {
			resultMap.put(t, volumePerBucket);
		}
		return resultMap;
	}
	
	private BigDecimal toPrice(Contract c, Integer ticks) {
		return BigDecimal.valueOf(c.priceTick() * ticks).setScale(c.pricePrecision(), RoundingMode.HALF_UP);
	}

	
	/*******************************************************************************************************************************/
	/**
	 * 该方法用于展示自定义指标
	 * 该指标算法与前端多图同列的指标算法基本一致
	 */
	@Override
	public List<ModuleIndicatorDescription> customIndicators(){
		ContractSimpleInfo csi = ContractSimpleInfo.builder().id(c.contractId()).unifiedSymbol(c.unifiedSymbol())
				.name(c.name()).symbol(c.symbol()).channelType(c.channelType())
				.dueDate(c.lastTradeDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.precision(c.pricePrecision()).build();
		String indicatorName = "ZigZag";
		List<Entry> simpleEntries = barsToEntries(barBuff.toList());
		List<Entry> basicEntries = basicProcess(simpleEntries);
		List<Entry> resultEntries = iteration(params.iterations, basicEntries);
		Map<Long, Entry> timeEntryMap = resultEntries.stream().collect(Collectors.toMap(e -> e.timestamp, e -> e));
		List<TimeSeriesValue> values = barBuff.toList().stream()
				.map(bar -> {
					Entry point = timeEntryMap.get(bar.actionTimestamp());
					Double val = null;
					if (point != null) {
						if (point.high != null) {
							val = point.high;
						}
						if (point.low != null) {
							val = point.low;
						}
						return new TimeSeriesValue(val, point.timestamp);
					}
					return null;
				})
				.filter(Objects::nonNull)
				.toList();
		
		ModuleIndicatorDescription mid = ModuleIndicatorDescription.builder()
				.contract(csi)
				.name(indicatorName)
				.style(LineStyle.LINES)
				.values(values)
				.build();
		
		return List.of(mid);
	}
	
	/* 这个数据结构用来记录高低点 */
	private class Entry {
		private Double high;
		private Double low;
		private long timestamp;
		
		public Entry(Double high, Double low, long timestamp) {
			this.high = high;
			this.low = low;
			this.timestamp = timestamp;
		}
	}
	
	private final Entry dummyEntry = new Entry(Double.NaN, Double.NaN, 0L);
	
	private List<Entry> barsToEntries(List<Bar> bars){
		return bars.stream()
				.map(bar -> new Entry(bar.highPrice(), bar.lowPrice(), bar.actionTimestamp()))
				.toList();
	}
	
	private List<Entry> basicProcess(List<Entry> entries){
		List<Entry> result = new LinkedList<>();
		for(int i=0; i<entries.size(); i++) {
			Entry prev = i == 0 ? dummyEntry : entries.get(i - 1);
			Entry next = i == entries.size() - 1 ? dummyEntry : entries.get(i + 1);
			Entry curr = entries.get(i);
			Double high = isPeak(curr.high, prev.high, next.high) ? curr.high : null;
			Double low = isTrough(curr.low, prev.low, next.low) ? curr.low : null;
			
			if(Objects.nonNull(high) && Objects.nonNull(low)) {
				if(prev.low > next.high) {
					high = null;
				} else if(prev.high < next.low) {
					low = null;
				} else {
					high = null;
					low = null;
				}
			}
			
			// 有可能一个K线同时出现符合条件的高低点标记，目测判断，这种情况一般不会出现真正的高点或低点，因此概率上可以作忽略处理
			if(Objects.isNull(high) && Objects.isNull(low)) {
				continue;
			} 
			result.add(new Entry(high, low, curr.timestamp));
		}
		return distinct(result);
	}
	
	private List<Entry> iteration(int times, List<Entry> entries) {
		if(times == 0) 
			return entries;
		return iteration(times - 1, distinct(makeData(entries)));
	}
	
	private Entry maxItem(Set<Entry> items) {
		return items.stream().sorted((a, b) -> a.high > b.high ? -1 : 1).toList().getFirst();
	}
	
	private Entry minItem(Set<Entry> items) {
		return items.stream().sorted((a, b) -> a.low < b.low ? -1 : 1).toList().getFirst();
	}
	
	/* 完成基础数组的去重工作（例如，[high, low, low, low, high]，会把三个low去重取最低值，反之亦然） */
	private List<Entry> distinct(List<Entry> entries) {
		LinkedList<Entry> result = new LinkedList<>();
		Set<Entry> bufEntries = new HashSet<>();
		for(int i=0; i<entries.size(); i++) {
			Entry curr = entries.get(i);
			Entry next = i == entries.size() - 1 ? dummyEntry : entries.get(i + 1);
			
			bufEntries.add(curr);
			if(Objects.nonNull(curr.high) && (next == dummyEntry || Objects.isNull(next.high))) {
				result.add(maxItem(bufEntries));
				bufEntries.clear();
			}
			if(Objects.nonNull(curr.low) && (next == dummyEntry || Objects.isNull(next.low))) {
				result.add(minItem(bufEntries));
				bufEntries.clear();
			}
		}
		
		if(!bufEntries.isEmpty()) {
			if(Objects.nonNull(result.peekLast().high)) {
				result.add(minItem(bufEntries));
			} else {
				result.add(maxItem(bufEntries));
			}
		}
		
		return result;
	}
	
	/* 按照“高点前后无更高点，低点前后无更低点”原则重新划定高低点 */
	/* 输入数组已经经过初步处理，保证了高低价间隔出现，例如[high, low, high, low, high] */
	private List<Entry> makeData(List<Entry> entries) {
		List<Entry> result = new LinkedList<>();
		for(int i=0; i<entries.size(); i++) {
			Entry prev = i < 2 ? dummyEntry : entries.get(i - 2);
			Entry next = i > entries.size() - 3 ? dummyEntry : entries.get(i + 2);
			Entry curr = entries.get(i);
			
			if(Objects.nonNull(curr.high) && isPeak(curr.high, prev.high, next.high)
					|| Objects.nonNull(curr.low) && isTrough(curr.low, prev.low, next.low)) {
				result.add(curr);
			}
		}
		return result;
	}
	
	private boolean isPeak(Double current, Double prev, Double next) {
		if(Double.isNaN(prev) && Double.isNaN(next)) {
			return true;
		} else if(Double.isNaN(prev)) {
			return current > next;
		} else if(Double.isNaN(next))	{
			return current > prev;
		}
		return current > prev && current >= next || current >= prev && current > next;
	}
	
	private boolean isTrough(Double current, Double prev, Double next) {
		if(Double.isNaN(prev) && Double.isNaN(next)) {
			return true;
		} else if(Double.isNaN(prev)) {
			return current < next;
		} else if(Double.isNaN(next))	{
			return current < prev;
		}
		return current < prev && current <= next || current <= prev && current < next;
	}

	
	/*******************************************************************************************************************************/
	
	public static class InitParams extends DynamicParams {			
		
		@Setting(label="迭代次数", order=0)
		private int iterations = 2;
		
		@Setting(label="价格区间范围", type = FieldType.NUMBER, order=10, unit = "个K线")
		private int refSize = 100;
	}

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		this.params = (InitParams) params;
	}

	@Override
	public String name() {
		return NAME;
	}
	
}
