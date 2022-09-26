package tech.quantit.northstar.strategy.api.indicator.complex;

import static tech.quantit.northstar.strategy.api.indicator.function.AverageFunctions.EMA;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.google.common.util.concurrent.AtomicDouble;

import tech.quantit.northstar.common.model.BarWrapper;
import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;
import tech.quantit.northstar.strategy.api.indicator.function.ComputeFunctions;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * DIFF : EMA(CLOSE,SHORT) - EMA(CLOSE,LONG);//短周期与长周期的收盘价的指数平滑移动平均值做差。
 * DEA  : EMA(DIFF,M);//DIFF的M个周期指数平滑移动平均
 * @author KevinHuangwl
 *
 */
public class MACD {

	private static final TimeSeriesValue TV_PLACEHOLDER = new TimeSeriesValue(0, 0);
	private int fast;
	private int slow;
	private int m;
	
	
	/**
	 * 创建MACD指标线生成器
	 * @param fast	快线周期
	 * @param slow	慢线周期
	 * @param m		移动平均周期
	 */
	public MACD(int fast, int slow, int m) {
		this.fast = fast;
		this.slow = slow;
		this.m = m;
	}
	
	/**
	 * 创建MACD指标线生成器
	 * @param fast
	 * @param slow
	 * @param m
	 * @return
	 */
	public static MACD of(int fast, int slow, int m) {
		return new MACD(fast, slow, m);
	}
	
	/**
	 * 获取DIFF线计算函数
	 * DIFF : EMA(CLOSE,SHORT) - EMA(CLOSE,LONG);//短周期与长周期的收盘价的指数平滑移动平均值做差。
	 * @return
	 */
	public TimeSeriesUnaryOperator diff() {
		return diff(EMA(this.fast), EMA(this.slow));
	}
	
	/**
	 * 获取DEA线计算函数
	 * DEA  : EMA(DIFF,M);//DIFF的M个周期指数平滑移动平均
	 * @return
	 */
	public TimeSeriesUnaryOperator dea() {
		return dea(EMA(this.fast), EMA(this.slow), this.m);
	}
	
	/**
	 * 获取红绿柱计算函数
	 * @return
	 */
	public TimeSeriesUnaryOperator post() {
		return post(diff(), dea());
	}
	
	/**
	 * 获取MACD红绿柱背离计算函数
	 * @param postFunction	红绿柱计算函数
	 * @return
	 */
	public TimeSeriesUnaryOperator divergence() {
		final LinkedList<Double> last3Bars = new LinkedList<>();
		final LinkedList<TimeSeriesValue> last3Posts = new LinkedList<>();
		final LinkedList<TimeSeriesValue> last3Diffs = new LinkedList<>();
		final TimeSeriesUnaryOperator postFunction = post();
		final TimeSeriesUnaryOperator diffFunction = diff();
		final AtomicDouble lastSectionHighestClose = new AtomicDouble(Double.MIN_VALUE);
		final AtomicDouble sectionHighestClose = new AtomicDouble(Double.MIN_VALUE);
		final AtomicDouble lastSectionRedArea = new AtomicDouble();
		final AtomicDouble sectionRedArea = new AtomicDouble(); 
		final AtomicDouble lastSectionLowestClose = new AtomicDouble(Double.MAX_VALUE);
		final AtomicDouble sectionLowestClose = new AtomicDouble(Double.MAX_VALUE);
		final AtomicDouble lastSectionGreenArea = new AtomicDouble();
		final AtomicDouble sectionGreenArea = new AtomicDouble();
		final AtomicInteger lastRedDivergenceCount = new AtomicInteger();
		final AtomicInteger lastGreenDivergenceCount = new AtomicInteger();
		final AtomicInteger divergenceCount = new AtomicInteger();
		return tv -> {
			/*********************************************************/
			/** 背离定义：MACD红绿柱的顶点对应的价格不为最值，视为背离。				**/
			/** 顶点的确认：红柱的中间值大于两边值，绿柱的中间值大于两边值。			**/
			/** 背离量化值：在同一块红绿柱区域，每个顶点+1；在不同一块红绿柱区域+10。		**/
			/** 算法优化：当DIFF大于零时，只计算多头背离；当DIFF小于零时，只计算空头背离。**/
			/*********************************************************/
			final int SECTION_OFFSET = 10;	// 跨区值
			final int OFFSET = 1;			// 同区值
			
			if(tv.isUnsettled()) {
				return TV_PLACEHOLDER;	// 实时值更新不适用于计算背离值，背离计算值必须是确定值
			}
			TimeSeriesValue postVal = postFunction.apply(tv);
			TimeSeriesValue diffVal = diffFunction.apply(tv);
			last3Bars.offer(tv.getValue());
			last3Posts.offer(postVal);
			last3Diffs.offer(diffVal);
			if(last3Bars.size() > 3) {
				last3Bars.pollFirst();
				last3Posts.pollFirst();
				last3Diffs.pollFirst();
			} else {
				return TV_PLACEHOLDER;
			}
			
			// 当红绿柱转换时，背离值清零，并重置区域最值
			if(last3Posts.get(2).getValue() * last3Posts.get(1).getValue() < 0) {
				if(postVal.getValue() > 0) {
					lastGreenDivergenceCount.set(divergenceCount.get());
					lastSectionHighestClose.set(sectionHighestClose.get());
					lastSectionLowestClose.set(sectionLowestClose.get());
					sectionHighestClose.set(Double.MIN_VALUE);
					sectionLowestClose.set(Double.MAX_VALUE);
					lastSectionRedArea.set(sectionRedArea.get());
					sectionRedArea.set(0);
				} else {
					lastRedDivergenceCount.set(divergenceCount.get());
					lastSectionHighestClose.set(sectionHighestClose.get());
					lastSectionLowestClose.set(sectionLowestClose.get());
					sectionHighestClose.set(Double.MIN_VALUE);
					sectionLowestClose.set(Double.MAX_VALUE);
					lastSectionGreenArea.set(sectionGreenArea.get());
					sectionGreenArea.set(0);
				}
				divergenceCount.set(0);
			}
			
			// 计算多头背离
			if(last3Diffs.get(0).getValue() > 0 && last3Diffs.get(1).getValue() > 0 && last3Diffs.get(2).getValue() > 0
					&& last3Posts.get(0).getValue() > 0 && last3Posts.get(1).getValue() > 0 && last3Posts.get(2).getValue() > 0) {
				if(last3Posts.get(0).getValue() < last3Posts.get(1).getValue() && last3Posts.get(1).getValue() > last3Posts.get(2).getValue()) {
					double area = last3Posts.stream().mapToDouble(TimeSeriesValue::getValue).sum(); 
					
					if(area < lastSectionRedArea.get() && sectionRedArea.get() == 0 && last3Bars.get(1) > lastSectionHighestClose.get()) {
						divergenceCount.getAndAdd(SECTION_OFFSET);
					}
					
					if(area < sectionRedArea.get() && last3Bars.get(1) > sectionHighestClose.get()) {
						divergenceCount.getAndAdd(OFFSET);
					}
					sectionRedArea.set(area);
					sectionHighestClose.set(last3Bars.get(1));
				}
			}
			
			// 计算空头背离
			if(last3Diffs.get(0).getValue() < 0 && last3Diffs.get(1).getValue() < 0 && last3Diffs.get(2).getValue() < 0
					&& last3Posts.get(0).getValue() < 0 && last3Posts.get(1).getValue() < 0 && last3Posts.get(2).getValue() < 0) {
				if(last3Posts.get(0).getValue() > last3Posts.get(1).getValue() && last3Posts.get(1).getValue() < last3Posts.get(2).getValue()) {
					double area = - last3Posts.stream().mapToDouble(TimeSeriesValue::getValue).sum();
					
					if(area < lastSectionGreenArea.get() && sectionGreenArea.get() == 0 && last3Bars.get(1) < lastSectionLowestClose.get()) {
						divergenceCount.getAndAdd(-SECTION_OFFSET);
					}
					
					if(area < sectionGreenArea.get() && last3Bars.get(1) < sectionLowestClose.get()) {
						divergenceCount.getAndAdd(-OFFSET);
					}
					sectionGreenArea.set(area);
					sectionLowestClose.set(last3Bars.get(1));
				}
			}
			
			return new TimeSeriesValue(divergenceCount.get(), tv.getTimestamp(), tv.isUnsettled());
		};
	}
	
	/**
	 * 获取DIFF线计算函数（泛化计算任意两线的MACD）
	 * @param line1		快线计算函数 
	 * @param line2		慢线计算函数
	 * @return
	 */
	public static TimeSeriesUnaryOperator diff(TimeSeriesUnaryOperator line1, TimeSeriesUnaryOperator line2) {
		return ComputeFunctions.minus(line1, line2);
	}
	
	/**
	 * 获取DEA线计算函数（泛化计算任意两线的MACD）
	 * @param line1		快线计算函数 
	 * @param line2		慢线计算函数
	 * @param m			移动平均周期
	 * @return
	 */
	public static TimeSeriesUnaryOperator dea(TimeSeriesUnaryOperator line1, TimeSeriesUnaryOperator line2, int m) {
		return diff(line1, line2).andThen(EMA(m));
	}
	
	/**
	 * 获取MACD红绿柱计算函数（泛化计算任意两线的MACD）
	 * @param diff		快线计算函数 
	 * @param dea		慢线计算函数
	 * @return
	 */
	public static TimeSeriesUnaryOperator post(TimeSeriesUnaryOperator diff, TimeSeriesUnaryOperator dea) {
		return tv -> {
			TimeSeriesValue difVal = diff.apply(tv);
			TimeSeriesValue deaVal = dea.apply(tv);
			return new TimeSeriesValue(2 * (difVal.getValue() - deaVal.getValue()), tv.getTimestamp(), tv.isUnsettled());
		};
	}
	
	/**
	 * 获取DIFF线计算函数（泛化计算任意两线的MACD）
	 * @param line1		快线计算函数 
	 * @param line2		慢线计算函数
	 * @return
	 */
	public static Function<BarWrapper, TimeSeriesValue> diff(Function<BarWrapper, TimeSeriesValue> line1, Function<BarWrapper, TimeSeriesValue> line2) {
		return ComputeFunctions.diff(line1, line2);
	}
	
	/**
	 * 获取DEA线计算函数（泛化计算任意两线的MACD）
	 * @param line1		快线计算函数 
	 * @param line2		慢线计算函数
	 * @param m			移动平均周期
	 * @return
	 */
	public static Function<BarWrapper, TimeSeriesValue> dea(Function<BarWrapper, TimeSeriesValue> line1, Function<BarWrapper, TimeSeriesValue> line2, int m) {
		return diff(line1, line2).andThen(EMA(m));
	}
	
	/**
	 * 获取MACD红绿柱计算函数（泛化计算任意两线的MACD）
	 * @param diff		快线计算函数 
	 * @param dea		慢线计算函数
	 * @return
	 */
	public static Function<BarWrapper, TimeSeriesValue> post(Function<BarWrapper, TimeSeriesValue> diff, Function<BarWrapper, TimeSeriesValue> dea){
		return bar -> {
			TimeSeriesValue difVal = diff.apply(bar);
			TimeSeriesValue deaVal = dea.apply(bar);
			return new TimeSeriesValue(2 * (difVal.getValue() - deaVal.getValue()), bar.getBar().getActionTimestamp(), bar.isUnsettled());
		};
	}
	
	/**
	 * 获取MACD红绿柱背离计算函数（泛化计算任意两线的MACD）
	 * @param line1		快线计算函数 
	 * @param line2		慢线计算函数
	 * @param m			移动平均周期
	 * @return
	 */
	public static Function<BarWrapper, TimeSeriesValue> divergence(Function<BarWrapper, TimeSeriesValue> post, Function<BarWrapper, TimeSeriesValue> diff){
		final LinkedList<BarField> last3Bars = new LinkedList<>();
		final LinkedList<TimeSeriesValue> last3Posts = new LinkedList<>();
		final LinkedList<TimeSeriesValue> last3Diffs = new LinkedList<>();
		final AtomicDouble lastSectionHighestClose = new AtomicDouble(Double.MIN_VALUE);
		final AtomicDouble sectionHighestClose = new AtomicDouble(Double.MIN_VALUE);
		final AtomicDouble lastSectionRedArea = new AtomicDouble();
		final AtomicDouble sectionRedArea = new AtomicDouble(); 
		final AtomicDouble lastSectionLowestClose = new AtomicDouble(Double.MAX_VALUE);
		final AtomicDouble sectionLowestClose = new AtomicDouble(Double.MAX_VALUE);
		final AtomicDouble lastSectionGreenArea = new AtomicDouble();
		final AtomicDouble sectionGreenArea = new AtomicDouble();
		return bar -> {
			/*********************************************************/
			/** 背离定义：MACD红绿柱的顶点对应的价格不为最值，视为背离。				**/
			/** 顶点的确认：红柱的中间值大于两边值，绿柱的中间值大于两边值。			**/
			/** 背离量化值：在同一块红绿柱区域，每个顶点+1；在不同一块红绿柱区域+10。		**/
			/** 算法优化：当DIFF大于零时，只计算多头背离；当DIFF小于零时，只计算空头背离。**/
			/*********************************************************/
			final int SECTION_OFFSET = 10;	// 跨区值
			final int OFFSET = 1;			// 同区值
			int divergenceCount = 0;	
			
			if(bar.isUnsettled()) {
				return TV_PLACEHOLDER;	// 实时值更新不适用于计算背离值，背离计算值必须是确定值
			}
			TimeSeriesValue postVal = post.apply(bar);
			TimeSeriesValue diffVal = diff.apply(bar);
			last3Bars.offer(bar.getBar());
			last3Posts.offer(postVal);
			last3Diffs.offer(diffVal);
			if(last3Bars.size() > 3) {
				last3Bars.pollFirst();
				last3Posts.pollFirst();
				last3Diffs.pollFirst();
			}
			
			// 当DIFF上穿0轴或者下穿0轴时，背离值清零，并重置区域最值
			if(last3Diffs.get(0).getValue() * last3Diffs.get(1).getValue() < 0) {
				divergenceCount = 0;
				if(diffVal.getValue() > 0) {
					lastSectionHighestClose.set(sectionHighestClose.get());
					sectionHighestClose.set(Double.MIN_VALUE);
					lastSectionRedArea.set(sectionRedArea.get());
					sectionRedArea.set(0);
				} else {
					lastSectionLowestClose.set(sectionLowestClose.get());
					sectionLowestClose.set(Double.MAX_VALUE);
					lastSectionRedArea.set(sectionGreenArea.get());
					sectionGreenArea.set(0);
				}
			}
			
			if(last3Diffs.get(0).getValue() > 0 && last3Diffs.get(1).getValue() > 0 && last3Diffs.get(2).getValue() > 0) {
				// 计算多头背离
				if(last3Diffs.get(0).getValue() < last3Diffs.get(1).getValue() && last3Diffs.get(1).getValue() > last3Diffs.get(2).getValue()) {
					double area = last3Diffs.stream().mapToDouble(TimeSeriesValue::getValue).sum(); 
					
					if(area < lastSectionRedArea.get() && last3Bars.get(1).getClosePrice() > lastSectionHighestClose.get()) {
						divergenceCount += SECTION_OFFSET;
					} else if(area < sectionRedArea.get() && last3Bars.get(1).getClosePrice() > sectionHighestClose.get()) {
						divergenceCount += OFFSET;
					}
					sectionRedArea.set(area);
					sectionHighestClose.set(last3Bars.get(1).getClosePrice());
				}
			}
			
			if(last3Diffs.get(0).getValue() < 0 && last3Diffs.get(1).getValue() < 0 && last3Diffs.get(2).getValue() < 0) {
				// 计算空头背离
				if(last3Diffs.get(0).getValue() > last3Diffs.get(1).getValue() && last3Diffs.get(1).getValue() < last3Diffs.get(2).getValue()) {
					double area = - last3Diffs.stream().mapToDouble(TimeSeriesValue::getValue).sum();
					
					if(area < lastSectionGreenArea.get() && last3Bars.get(1).getClosePrice() < lastSectionLowestClose.get()) {
						divergenceCount += SECTION_OFFSET;
					} else if(area < sectionGreenArea.get() && last3Bars.get(1).getClosePrice() < sectionLowestClose.get()) {
						divergenceCount += OFFSET;
					}
					sectionGreenArea.set(area);
					sectionLowestClose.set(last3Bars.get(1).getClosePrice());
				}
			}
			
			return new TimeSeriesValue(divergenceCount, bar.getBar().getActionTimestamp(), bar.isUnsettled());
		};
	}
}
