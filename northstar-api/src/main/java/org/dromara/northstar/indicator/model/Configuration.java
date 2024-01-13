package org.dromara.northstar.indicator.model;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.constant.PeriodUnit;
import org.dromara.northstar.indicator.constant.ValueType;

import lombok.Builder;

/**
 * 指标配置类
 * @author KevinHuangwl
 *
 */
@Builder(toBuilder = true)
public record Configuration(
		/**
		 * 显示名称
		 */
		String indicatorName,
		/**
		 * 绑定合约
		 */
		Contract contract,
		/**
		 * N个周期
		 */
		Integer numOfUnits, 
		/**
		 * 周期单位
		 */
		PeriodUnit period,
		/**
		 * 值类型
		 */
		ValueType valueType, 
		/**
		 * 可回溯长度
		 */
		Integer cacheLength, 
		/**
		 * 跨周期指标映射到每根K线
		 */
		Boolean ifPlotPerBar, 
		/**
		 * 是否在模组图表中显示
		 */
		Boolean visible){
	
	public Configuration {
		if(numOfUnits == null)
			numOfUnits = 1;
		if(period == null)
			period = PeriodUnit.MINUTE;
		if(valueType == null)
			valueType = ValueType.CLOSE;
		if(cacheLength == null)
			cacheLength = 16;
		if(ifPlotPerBar == null)
			ifPlotPerBar = false;
		if(visible == null)
			visible = true;
		if (indicatorName == null)
			indicatorName = "";
        if (contract == null) 
            throw new IllegalArgumentException(indicatorName + "未绑定计算合约");
    }
	
	public String indicatorID() {
		return String.format("%s_%d%s", indicatorName(), numOfUnits(), period().symbol());
	}
}