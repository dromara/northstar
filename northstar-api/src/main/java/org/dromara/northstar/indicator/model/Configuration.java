package org.dromara.northstar.indicator.model;

import java.util.Optional;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.constant.PeriodUnit;
import org.dromara.northstar.indicator.constant.ValueType;
import org.springframework.util.Assert;

/**
 * 指标配置类
 * @author KevinHuangwl
 *
 */
public record Configuration(String indicatorName, Contract contract, int numOfUnits, PeriodUnit period,
		ValueType valueType, int cacheLength, boolean ifPlotPerBar, boolean visible){
	
	public static Builder builder() {
		return new Builder();
	}
	
	public Builder toBuilder() {
		Builder b = builder();
		b.indicatorName = this.indicatorName;
		b.contract = this.contract;
		b.numOfUnits = this.numOfUnits;
		b.period = this.period;
		b.valueType = this.valueType;
		b.cacheLength = this.cacheLength;
		b.ifPlotPerBar = this.ifPlotPerBar;
		return b; 
	}
	
	public static class Builder {
		/**
		 * 显示名称
		 */
		private String indicatorName;
		/**
		 * 绑定合约
		 */
		private Contract contract;
		/**
		 * N个周期
		 */
		private int numOfUnits = 1;
		/**
		 * 周期单位
		 */
		private PeriodUnit period = PeriodUnit.MINUTE;
		/**
		 * 值类型
		 */
		private ValueType valueType = ValueType.CLOSE;
		/**
		 * 可回溯长度
		 */
		private int cacheLength = 16;
		/**
		 * 跨周期指标映射到每根K线
		 */
		private boolean ifPlotPerBar;
		/**
		 * 是否在模组图表中显示
		 */
		private boolean visible = true;
		
		public Builder indicatorName(String name) {
			this.indicatorName = name;
			return this;
		}
		
		public Builder contract(Contract contract) {
			this.contract = contract;
			return this;
		}
		
		public Builder numOfUnits(int numOfUnits) {
			this.numOfUnits = numOfUnits;
			return this;
		}
		
		public Builder period(PeriodUnit period) {
			this.period = period;
			return this;
		}
		
		public Builder valueType(ValueType valueType) {
			this.valueType = valueType;
			return this;
		}
		
		public Builder cacheLength(int cacheLength) {
			this.cacheLength = cacheLength;
			return this;
		}
		
		public Builder ifPlotPerBar(boolean ifPlotPerBar) {
			this.ifPlotPerBar = ifPlotPerBar;
			return this;
		}
		
		public Builder visible(boolean visible) {
			this.visible = visible;
			return this;
		}
		
		public Configuration build() {
			Assert.notNull(contract, Optional.ofNullable(indicatorName).orElse("") + " 未绑定计算合约");
			return new Configuration(indicatorName, contract, numOfUnits, period, valueType, cacheLength, ifPlotPerBar, visible);
		}
	}
}