package org.dromara.northstar.ai;

import java.util.Arrays;
import java.util.Objects;

import lombok.Builder;

/**
 * 样本数据
 * @auth KevinHuangwl
 */
@Builder
public record SampleData(
		/**
		 * 样本日期
		 */
		String actionDate,
		/**
		 * 样本时间
		 */
		String actionTime,
		/**
		 * 环境状态
		 */
		double[] states,
		/**
		 * 市场价
		 */
		double marketPrice
	) {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(states);
		result = prime * result + Objects.hash(actionDate, actionTime, marketPrice);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SampleData other = (SampleData) obj;
		return Objects.equals(actionDate, other.actionDate) && Objects.equals(actionTime, other.actionTime)
				&& Double.doubleToLongBits(marketPrice) == Double.doubleToLongBits(other.marketPrice)
				&& Arrays.equals(states, other.states);
	}

	@Override
	public String toString() {
		return "SampleData [actionDate=" + actionDate + ", actionTime="
				+ actionTime + ", states=" + Arrays.toString(states) + ", marketPrice=" + marketPrice + "]";
	}
	
}
