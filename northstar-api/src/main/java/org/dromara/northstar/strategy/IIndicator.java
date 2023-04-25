package org.dromara.northstar.strategy;

import java.util.List;

import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.model.TimeSeriesValue;

@Deprecated
public interface IIndicator extends TickDataAware, BarDataAware, MergedBarListener {

	/**
	 * 获取指标回溯值
	 * @param numOfStepBack		回溯步长，取值范围为(-size, 0]。0代表当前值，-1代表回溯上一步，-2代表回溯上两步，如此类推
	 * @return
	 */
	public double value(int numOfStepBack);
	
	/**
	 * 获取指标回溯值
	 * @param numOfStepBack
	 * @return
	 */
	public TimeSeriesValue timeSeriesValue(int numOfStepBack);
	
	/**
	 * 指标是否已完成初始化
	 * @return
	 */
	public boolean isReady();
	
	/**
	 * 指标绑定合约
	 * @return
	 */
	public String bindedUnifiedSymbol();
	
	/**
	 * 获取最高值的回溯步长
	 * @return		
	 */
	public int highestPosition();
	
	/**
	 * 获取最低值的回溯步长
	 * @return		
	 */
	public int lowestPosition();
	
	/**
	 * 获取系列值
	 * @return
	 */
	public List<TimeSeriesValue> getData();
	
	/**
	 * 指标名称
	 * @return
	 */
	public String name();
	
	/**
	 * 跨周期
	 * @return
	 */
	public boolean ifPlotPerBar();
}
