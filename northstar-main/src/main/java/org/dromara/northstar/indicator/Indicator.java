package org.dromara.northstar.indicator;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.dromara.northstar.common.model.BarWrapper;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.strategy.IIndicator;
import org.dromara.northstar.strategy.constant.ValueType;
import org.dromara.northstar.strategy.model.Configuration;

import reactor.core.publisher.Flux;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 行情指标
 * 主要关注指标与合约的绑定关系、指标长度、指标更新处理
 * @author KevinHuangwl
 *
 */
public class Indicator implements IIndicator {
	
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
	
	private Configuration config;
	
	public Indicator(Configuration config, ValueType valType, UnaryOperator<TimeSeriesValue> valueUpdateHandler) {
		this.size = config.getIndicatorRefLength();
		this.config = config;
		this.unifiedSymbol = config.getBindedContract().getUnifiedSymbol();
		this.plotPerBar = config.isPlotPerBar();
		this.ibg = new InstantBarGenerator(config.getBindedContract());
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
	
	public Indicator(Configuration config, Function<BarWrapper, TimeSeriesValue> valueUpdateHandler) {
		this.size = config.getIndicatorRefLength();
		this.config = config;
		this.unifiedSymbol = config.getBindedContract().getUnifiedSymbol();
		this.plotPerBar = config.isPlotPerBar();
		this.ibg = new InstantBarGenerator(config.getBindedContract());
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
	@Override
	public double value(int numOfStepBack) {
		return timeSeriesValue(numOfStepBack).getValue();
	}
	
	/**
	 * 获取指标回溯值
	 * @param numOfStepBack
	 * @return
	 */
	@Override
	public TimeSeriesValue timeSeriesValue(int numOfStepBack) {
		if(Math.abs(numOfStepBack) > size) {
			throw new IllegalArgumentException("回溯步长[" + numOfStepBack + "]超过记录长度");
		}
		return refVals.get(numOfStepBack);
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
		case BARYCENTER -> (bar.getHighPrice() + bar.getLowPrice() + bar.getClosePrice() * 2) / 4;
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
	protected synchronized void updateVal(TimeSeriesValue tv) {
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
	@Override
	public boolean isReady() {
		return actualUpdate >= size;
	}
	
	/**
	 * 指标绑定合约
	 * @return
	 */
	@Override
	public String bindedUnifiedSymbol() {
		return unifiedSymbol;
	}
	
	/**
	 * 获取最高值的回溯步长
	 * @return		
	 */
	@Override
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
	@Override
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
	@Override
	public List<TimeSeriesValue> getData(){
		return Stream.of(refVals.toArray())
				.map(TimeSeriesValue.class::cast)
				.toList();
	}
	
	/**
	 * 指标名称
	 * @return
	 */
	@Override
	public String name() {
		return config.getIndicatorName();
	}
	
	/**
	 * 跨周期
	 * @return
	 */
	@Override
	public boolean ifPlotPerBar() {
		return plotPerBar;
	}
	
	private interface BarListener {
		
		void onBar(Object obj);
	}
}
