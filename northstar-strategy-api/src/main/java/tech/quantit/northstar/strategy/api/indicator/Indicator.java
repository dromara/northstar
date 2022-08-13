package tech.quantit.northstar.strategy.api.indicator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import tech.quantit.northstar.common.constant.IndicatorType;
import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.utils.collection.RingArray;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 行情指标
 * 主要关注指标与合约的绑定关系、指标长度、指标更新处理
 * @author KevinHuangwl
 *
 */
@Slf4j
public class Indicator {
	
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
	
	public Indicator(String unifiedSymbol, int size, ValueType valType, UnaryOperator<TimeSeriesValue> valueUpdateHandler) {
		refVals = new RingArray<>(size);
		for(int i=0; i<size; i++) {
			refVals.update(new TimeSeriesValue(0, 0));
		}
		this.unifiedSymbol = unifiedSymbol;
		this.valType = valType;
		this.size = size;
		
		Flux.push(sink -> 
			barListener = sink::next
		)
		.map(TimeSeriesValue.class::cast)
		.map(valueUpdateHandler)
		.subscribe(this::updateVal);
	}
	
	public Indicator(String unifiedSymbol, int size, Function<BarField, TimeSeriesValue> valueUpdateHandler) {
		refVals = new RingArray<>(size);
		for(int i=0; i<size; i++) {
			refVals.update(new TimeSeriesValue(0, 0));
		}
		this.unifiedSymbol = unifiedSymbol;
		this.valType = ValueType.NOT_SET;
		this.size = size;
		
		Flux.push(sink -> 
			barListener = sink::next
		)
		.map(BarField.class::cast)
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
		return valueWithTime(numOfStepBack).getValue();
	}
	
	/**
	 * 获取指标回溯值
	 * @param numOfStepBack
	 * @return
	 */
	public TimeSeriesValue valueWithTime(int numOfStepBack) {
		if(Math.abs(numOfStepBack) > size) {
			throw new IllegalArgumentException("回溯步长超过记录长度");
		}
		return refVals.get(-numOfStepBack);
	}
	
	/**
	 * 获取指标回溯值 
	 * @param time		指标值对应的时间戳
	 * @return
	 */
	public Optional<Double> valueOn(long time){
		Map<Long, Double> valMap = new HashMap<>();
		for(Object obj : refVals.toArray()) {
			TimeSeriesValue val = (TimeSeriesValue) obj;
			valMap.put(val.getTimestamp(), val.getValue());
		}
		return Optional.ofNullable(valMap.get(time)); 
	}
	
	/**
	 * 依赖BAR的值更新 
	 */
	public void onBar(BarField bar) {
		if(!bar.getUnifiedSymbol().equals(unifiedSymbol)) {
			return;
		}
		log.trace("{} -> {}", this, bar);
		if(valType == ValueType.NOT_SET) {
			barListener.onBar(bar);
			return;
		}
		
		double barVal = switch(valType) {
		case HIGH -> bar.getHighPrice();
		case CLOSE -> bar.getClosePrice();
		case LOW -> bar.getLowPrice();
		case OPEN -> bar.getOpenPrice();
		case OPEN_INTEREST -> bar.getOpenInterestDelta();
		case VOL -> bar.getVolumeDelta();
		default -> throw new IllegalArgumentException("Unexpected value: " + valType);
		};
		barListener.onBar(new TimeSeriesValue(barVal, bar.getActionTimestamp()));
	}
	
	/**
	 * 值更新
	 * @param newVal
	 */
	public void updateVal(TimeSeriesValue tv) {
		if(tv.getTimestamp() == 0) 	return;	// 时间戳为零会视为无效记录 
		refVals.update(tv);
		actualUpdate++;
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
	 * 指标类型
	 * @return
	 */
	public IndicatorType getType() {
		return switch(valType) {
		case OPEN,CLOSE,HIGH,LOW -> IndicatorType.PRICE_BASE;
		case VOL -> IndicatorType.VOLUME_BASE;
		case OPEN_INTEREST -> IndicatorType.OPEN_INTEREST_BASE;
		default -> IndicatorType.UNKNOWN;
		};
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
		OPEN_INTEREST;
	}
	
	private interface BarListener {
	
		void onBar(Object obj);
	}
	
}
