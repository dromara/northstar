package tech.quantit.northstar.strategy.api.indicator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.model.BarWrapper;
import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.BarDataAware;
import tech.quantit.northstar.strategy.api.MergedBarListener;
import tech.quantit.northstar.strategy.api.utils.bar.InstantBarGenerator;
import tech.quantit.northstar.strategy.api.utils.collection.RingArray;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 行情指标
 * 主要关注指标与合约的绑定关系、指标长度、指标更新处理
 * @author KevinHuangwl
 *
 */
public class Indicator implements TickDataAware, BarDataAware, MergedBarListener {
	
	/**
	 * 指标历史记录
	 * 采用环型数组记录
	 */
	private RingArray<TimeSeriesValue> refVals;
	/**
	 * 指标取值类型
	 */
	private ValueType valType;
	
	private String unifiedSymbol;
	
	private int size;
	
	private int actualUpdate;
	
	private BarListener barListener;
	
	private InstantBarGenerator ibg;
	
	private boolean plotPerBar;
	
	private Indicator.Configuration config;
	
	public Indicator(Indicator.Configuration config, ValueType valType, UnaryOperator<TimeSeriesValue> valueUpdateHandler) {
		this.size = config.indicatorRefLength;
		this.config = config;
		this.unifiedSymbol = config.bindedContract.getUnifiedSymbol();
		this.plotPerBar = config.plotPerBar;
		this.ibg = new InstantBarGenerator(config.bindedContract);
		this.valType = valType;
		refVals = new RingArray<>(size);
		for(int i=0; i<size; i++) {
			refVals.update(new TimeSeriesValue(0, 0, false), false);
		}
		
		Flux.push(sink -> 
			barListener = sink::next
		)
		.map(TimeSeriesValue.class::cast)
		.map(valueUpdateHandler)
		.subscribe(this::updateVal);
	}
	
	public Indicator(Indicator.Configuration config, Function<BarWrapper, TimeSeriesValue> valueUpdateHandler) {
		this.size = config.indicatorRefLength;
		this.config = config;
		this.unifiedSymbol = config.bindedContract.getUnifiedSymbol();
		this.plotPerBar = config.plotPerBar;
		this.ibg = new InstantBarGenerator(config.bindedContract);
		this.valType = ValueType.NOT_SET;
		refVals = new RingArray<>(size);
		for(int i=0; i<size; i++) {
			refVals.update(new TimeSeriesValue(0, 0, false), false);
		}
		
		Flux.push(sink -> 
			barListener = sink::next
		)
		.map(BarWrapper.class::cast)
		.map(valueUpdateHandler)
		.subscribe(this::updateVal);
	}
	
	/**
	 * 获取指标回溯值
	 * @param numOfStepBack		回溯步长。0代表当前值，正数代表从近到远N个周期前的值，负数代表从远到近N个周期的值
	 * 例如有一个记录集 [77,75,80,99]， 当前指针是2，即值为80。
	 * 当回溯步长为0时，返回80；
	 * 当回溯步长为1时，返回75；
	 * 当回溯步长为2时，返回77；
	 * 当回溯步长为3时，返回99；
	 * 当回溯步长为-1时，返回99；
	 * 如此类推
	 * @return
	 */
	public double value(int numOfStepBack) {
		return timeSeriesValue(numOfStepBack).getValue();
	}
	
	/**
	 * 获取指标回溯值
	 * @param numOfStepBack
	 * @return
	 */
	public TimeSeriesValue timeSeriesValue(int numOfStepBack) {
		if(Math.abs(numOfStepBack) > size) {
			throw new IllegalArgumentException("回溯步长[" + numOfStepBack + "]超过记录长度");
		}
		return refVals.get(-numOfStepBack);
	}
	
	/**
	 * 获取指标回溯值 
	 * @param time		指标值对应的时间戳
	 * @return
	 */
	public Optional<Double> valueOn(long time){
		return timeSeriesValueOn(time).map(TimeSeriesValue::getValue); 
	}
	
	/**
	 * 获取指标回溯值
	 * @param time		指标值对应的时间戳
	 * @return
	 */
	public Optional<TimeSeriesValue> timeSeriesValueOn(long time){
		Map<Long, TimeSeriesValue> valMap = new HashMap<>();
		for(Object obj : refVals.toArray()) {
			TimeSeriesValue val = (TimeSeriesValue) obj;
			valMap.put(val.getTimestamp(), val);
		}
		return Optional.ofNullable(valMap.get(time));
	}
	
	@Override
	public void onMergedBar(BarField bar) {
		onBar(bar, false);
	}
	
	private void onBar(BarField bar, boolean isUnsettled) {
		if(!bar.getUnifiedSymbol().equals(unifiedSymbol)) {
			return;
		}
		if(valType == ValueType.NOT_SET) {
			barListener.onBar(new BarWrapper(bar, isUnsettled));
			return;
		}
		
		double barVal = switch(valType) {
		case HIGH -> bar.getHighPrice();
		case CLOSE -> bar.getClosePrice();
		case LOW -> bar.getLowPrice();
		case OPEN -> bar.getOpenPrice();
		case OI -> bar.getOpenInterest();
		case OI_DELTA -> bar.getOpenInterestDelta();
		case VOL -> bar.getVolume();
		default -> throw new IllegalArgumentException("Unexpected value: " + valType);
		};
		barListener.onBar(new TimeSeriesValue(barVal, bar.getActionTimestamp(), isUnsettled));
	}
	
	/**
	 * 依赖分钟BAR的临时值更新
	 */
	@Override
	public void onBar(BarField bar) {
		onBar(bar, true);
	}
	
	/**
	 * 依赖TICK的临时值更新
	 * @param tick
	 */
	@Override
	public synchronized void onTick(TickField tick) {
		if(!tick.getUnifiedSymbol().equals(unifiedSymbol)) {
			return;
		}
		ibg.update(tick).ifPresent(bar -> onBar(bar, true));
	}
	
	/**
	 * 值更新
	 * @param newVal
	 */
	public synchronized void updateVal(TimeSeriesValue tv) {
		if(tv.getTimestamp() == 0) 	return;	// 时间戳为零会视为无效记录 
		refVals.update(tv, tv.isUnsettled());
		if(!tv.isUnsettled()) {			
			actualUpdate++;
		}
	}
	
	/**
	 * 指标是否已完成初始化
	 * @return
	 */
	public boolean isReady() {
		return actualUpdate >= size;
	}
	
	/**
	 * 指标绑定合约
	 * @return
	 */
	public String bindedUnifiedSymbol() {
		return unifiedSymbol;
	}
	
	/**
	 * 获取最高值的回溯步长
	 * @return		
	 */
	public int highestPosition() {
		int stepback = 0;
		double highestVal = Double.MIN_VALUE;
		for(int i=0; i<size; i++) {
			if(value(i) > highestVal) {
				highestVal = value(i);
				stepback = i;
			}
		}
		return stepback;
	}
	
	/**
	 * 获取最低值的回溯步长
	 * @return		
	 */
	public int lowestPosition() {
		int stepback = 0;
		double lowestVal = Double.MAX_VALUE;
		for(int i=0; i<size; i++) {
			if(value(i) < lowestVal) {
				lowestVal = value(i);
				stepback = i;
			}
		}
		return stepback;
	}
	
	/**
	 * 获取系列值
	 * @return
	 */
	public List<TimeSeriesValue> getData(){
		return Stream.of(refVals.toArray())
				.map(TimeSeriesValue.class::cast)
				.toList();
	}
	
	/**
	 * 指标名称
	 * @return
	 */
	public String name() {
		return config.getIndicatorName();
	}
	
	/**
	 * 跨周期
	 * @return
	 */
	public boolean ifPlotPerBar() {
		return plotPerBar;
	}
	
	/**
	 * 指标取值类型
	 * @author KevinHuangwl
	 *
	 */
	public enum ValueType {
		/**
		 * 未设置
		 */
		NOT_SET,
		/**
		 * 最高价
		 */
		HIGH,
		/**
		 * 最低价
		 */
		LOW,
		/**
		 * 开盘价
		 */
		OPEN,
		/**
		 * 收盘价
		 */
		CLOSE,
		/**
		 * 成交量
		 */
		VOL,
		/**
		 * 持仓量
		 */
		OI,
		/**
		 * 持仓量变化
		 */
		OI_DELTA;
	}
	
	/**
	 * 周期单位
	 * @author KevinHuangwl
	 *
	 */
	public enum PeriodUnit{
		/**
		 * 分钟
		 */
		MINUTE("m"),
		/**
		 * 小时
		 */
		HOUR("hr"),
		/**
		 * 天
		 */
		DAY("d"),
		/**
		 * 周
		 */
		WEEK("wk"),
		/**
		 * 月
		 */
		MONTH("M");
		
		String symbol;
		private PeriodUnit(String unitSymbol) {
			symbol = unitSymbol;
		}
		
		public String symbol() {
			return symbol;
		}
	}
	
	private interface BarListener {
	
		void onBar(Object obj);
	}
	
	/**
	 * 指标配置
	 * @author KevinHuangwl
	 *
	 */
	@Builder(toBuilder = true)
	@Getter
	public static class Configuration {
		/**
		 * 显示名称
		 */
		private String indicatorName;
		/**
		 * 绑定合约
		 */
		@NonNull
		private ContractField bindedContract;
		/**
		 * N个周期
		 */
		@Builder.Default
		private int numOfUnits = 1;
		/**
		 * 周期单位
		 */
		@Builder.Default
		private PeriodUnit period = PeriodUnit.MINUTE;
		/**
		 * 可回溯长度
		 */
		@Builder.Default
		private int indicatorRefLength = 16;
		/**
		 * 跨周期指标映射到每根K线
		 */
		@Builder.Default
		private boolean plotPerBar = false;
		
		
		public String getIndicatorName() {
			return String.format("%s_%d%s", indicatorName, numOfUnits, period.symbol);
		}

		@Override
		public String toString() {
			return "Configuration [indicatorName=" + indicatorName + ", bindedContract=" + bindedContract
					+ ", numOfUnits=" + numOfUnits + ", period=" + period + ", indicatorRefLength=" + indicatorRefLength
					+ ", plotPerBar=" + plotPerBar + "]";
		}
		
	}

}
