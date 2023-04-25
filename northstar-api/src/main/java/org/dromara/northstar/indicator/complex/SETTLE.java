package org.dromara.northstar.indicator.complex;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.dromara.northstar.common.model.BarWrapper;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.indicator.function.AverageFunctions;

import com.google.common.util.concurrent.AtomicDouble;

/**
 * 日内成交量均价线
 * 对日内均价的各种用法做进一步封装（仅适用于15分钟及以下周期）
 * @author KevinHuangwl
 *
 */
@Deprecated
public class SETTLE {
	
	private static final TimeSeriesValue TV_PLACEHOLDER = new TimeSeriesValue(0, 0);
	
	/**
	 * 日内成交量加权均价线计算函数
	 * @return
	 */
	public static Function<BarWrapper, TimeSeriesValue> baseLine(){
		return AverageFunctions.SETTLE();
	}
	
	/**
	 * 最近N天的标准差（不包括当天）
	 * @param n		统计范围N天
	 * @return
	 */
	public static Function<BarWrapper, TimeSeriesValue> standardDeviation(int n){
		final LinkedList<String> tradeDateQ = new LinkedList<>();
		final Map<String, List<Double>> dateValueMap = new HashMap<>();
		final Function<BarWrapper, TimeSeriesValue> settleLine = baseLine();
		final AtomicDouble currentSTD = new AtomicDouble();
		return bar -> {
			if(bar.isUnsettled()) {
				return TV_PLACEHOLDER;
			}
			// 新交易日的值更新
			if(tradeDateQ.isEmpty() || !tradeDateQ.peekLast().equals(bar.getBar().getTradingDay())) {
				// 计算标准差
				if(!tradeDateQ.isEmpty() && !tradeDateQ.peekLast().equals(bar.getBar().getTradingDay())) {
					double sumSquare = dateValueMap.values().stream()
							.flatMap(Collection::stream)
							.mapToDouble(Double.class::cast)
							.sum();
					long numOfItem = dateValueMap.values().stream()
							.flatMap(Collection::stream)
							.count();
					currentSTD.set(Math.sqrt(sumSquare / (numOfItem - 1)));
				}
				// 当统计天数已满，需要移除旧数据
				if(tradeDateQ.size() == n) {					
					String date = tradeDateQ.pollFirst();
					dateValueMap.remove(date);
				}
				tradeDateQ.offerLast(bar.getBar().getTradingDay());
				dateValueMap.put(bar.getBar().getTradingDay(), new LinkedList<>());
			}
			
			TimeSeriesValue settleVal = settleLine.apply(bar);
			double weightedClose = (bar.getBar().getClosePrice() * 2 + bar.getBar().getHighPrice() + bar.getBar().getClosePrice()) / 4; 	// 加权重心价
			double variance = Math.pow(weightedClose - settleVal.getValue(), 2);
			dateValueMap.get(bar.getBar().getTradingDay()).add(variance);
			return new TimeSeriesValue(currentSTD.get(), bar.getBar().getActionTimestamp());
		};
	}
}
