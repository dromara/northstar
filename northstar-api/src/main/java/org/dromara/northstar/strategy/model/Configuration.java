package org.dromara.northstar.strategy.model;

import org.dromara.northstar.strategy.constant.PeriodUnit;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 指标配置
 * @author KevinHuangwl
 *
 */
@Deprecated
@Builder(toBuilder = true)
@Getter
public class Configuration {
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
		return String.format("%s_%d%s", indicatorName, numOfUnits, period.symbol());
	}

	@Override
	public String toString() {
		return "Configuration [indicatorName=" + indicatorName + ", bindedContract=" + bindedContract
				+ ", numOfUnits=" + numOfUnits + ", period=" + period + ", indicatorRefLength=" + indicatorRefLength
				+ ", plotPerBar=" + plotPerBar + "]";
	}
	
}