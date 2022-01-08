package tech.quantit.northstar.strategy.api.indicator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.strategy.api.BarDataAware;
import tech.quantit.northstar.strategy.api.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.utils.collection.RingArray;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 行情指标抽象类
 * @author KevinHuangwl
 *
 */
@Slf4j
public abstract class Indicator implements BarDataAware {
	
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
	
	protected Indicator(String unifiedSymbol, int size, ValueType valType) {
		refVals = new RingArray<>(size);
		for(int i=0; i<size; i++) {
			refVals.update(new TimeSeriesValue(0, 0));
		}
		this.unifiedSymbol = unifiedSymbol;
		this.valType = valType;
		this.size = size;
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
		if(Math.abs(numOfStepBack) > size) {
			throw new IllegalArgumentException("回溯步长超过记录长度");
		}
		return refVals.get(-numOfStepBack).getValue();
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
	
	
	@Override
	public void onBar(BarField bar) {
		if(!bar.getUnifiedSymbol().equals(unifiedSymbol)) {
			return;
		}
		log.trace("{} -> {}", this, bar);
		TimeSeriesValue tsv = refVals.get(1);
		tsv.setTimestamp(bar.getActionTimestamp());
		switch(valType) {
		case HIGH -> tsv.setValue(updateVal(bar.getHighPrice()));
		case CLOSE -> tsv.setValue(updateVal(bar.getClosePrice()));
		case LOW -> tsv.setValue(updateVal(bar.getLowPrice()));
		case OPEN -> tsv.setValue(updateVal(bar.getOpenPrice()));
		case OPEN_INTEREST -> tsv.setValue(updateVal(bar.getOpenInterestDelta()));
		case VOL -> tsv.setValue(updateVal(bar.getVolumeDelta()));
		default -> throw new IllegalArgumentException("Unexpected value: " + valType);
		}
		refVals.update(tsv);
	}
	
	public TimeSeriesValue highestVal() {
		TimeSeriesValue highest = null;
		for(Object obj : refVals.toArray()) {
			if(highest == null) {
				highest = (TimeSeriesValue) obj;
			} else {
				highest = highest.compareTo((TimeSeriesValue) obj) > 0 ? highest : (TimeSeriesValue) obj;
			}
		}
		return highest; 
	}
	
	public TimeSeriesValue lowestVal() {
		TimeSeriesValue lowest = null;
		for(Object obj : refVals.toArray()) {
			if(lowest == null) {
				lowest = (TimeSeriesValue) obj;
			} else {
				lowest = lowest.compareTo((TimeSeriesValue) obj) < 0 ? lowest : (TimeSeriesValue) obj;
			}
		}
		return lowest;
	}

	/**
	 * 指标更新值
	 * @param newVal
	 * @return
	 */
	protected abstract double updateVal(double newVal);
	
	/**
	 * 指标取值类型
	 * @author KevinHuangwl
	 *
	 */
	public enum ValueType {
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
}
