package org.dromara.northstar.indicator;

import org.dromara.northstar.strategy.constant.PeriodUnit;
import org.dromara.northstar.strategy.constant.ValueType;

import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 指标配置类
 * @author KevinHuangwl
 *
 */
public record Configuration(String indicatorName, ContractField contract, int numOfUnits, PeriodUnit period,
		ValueType valueType, int cacheLength, boolean ifPlotPerBar){
	
	public String indicatorName() {
		return String.format("%s_%d%s", indicatorName, numOfUnits, period.symbol());
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		/**
		 * 显示名称
		 */
		private String indicatorName;
		/**
		 * 绑定合约
		 */
		private ContractField contract;
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
		
		
		public Builder indicatorName(String name) {
			this.indicatorName = name;
			return this;
		}
		
		public Builder contract(ContractField contract) {
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
		
		public Configuration build() {
			return new Configuration(indicatorName, contract, numOfUnits, period, valueType, cacheLength, ifPlotPerBar);
		}
	}
}