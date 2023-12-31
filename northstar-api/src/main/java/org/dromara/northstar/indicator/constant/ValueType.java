package org.dromara.northstar.indicator.constant;

import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.model.core.Bar;

/**
 * 指标取值类型
 * @author KevinHuangwl
 *
 */
public enum ValueType {
	/**
	 * 最高价
	 */
	HIGH {
		@Override
		public double resolve(Bar bar) {
			return bar.highPrice();
		}
	},
	/**
	 * 最低价
	 */
	LOW {
		@Override
		public double resolve(Bar bar) {
			return bar.lowPrice();
		}
	},
	/**
	 * 开盘价
	 */
	OPEN {
		@Override
		public double resolve(Bar bar) {
			return bar.openPrice();
		}
	},
	/**
	 * 收盘价
	 */
	CLOSE {
		@Override
		public double resolve(Bar bar) {
			return bar.closePrice();
		}
	},
	/**
	 * 重心价
	 */
	BARYCENTER {
		@Override
		public double resolve(Bar bar) {
			return (bar.highPrice() + bar.closePrice() + bar.closePrice() * 2) / 4;
		}
	},
	/**
	 * 当日累计成交量
	 */
	VOL {
		@Override
		public double resolve(Bar bar) {
			return bar.volume();
		}
	},
	/**
	 * K线成交量
	 */
	VOL_DELTA {
		@Override
		public double resolve(Bar bar) {
			return bar.volumeDelta();
		}
	},
	/**
	 * 实际持仓量
	 */
	OI {
		@Override
		public double resolve(Bar bar) {
			return bar.openInterest();
		}
	},
	/**
	 * 持仓量变化
	 */
	OI_DELTA {
		@Override
		public double resolve(Bar bar) {
			return bar.openInterestDelta();
		}
	},
	/**
	 * 交易日
	 */
	TRADE_DATE {
		@Override
		public double resolve(Bar bar) {
			return Double.parseDouble(bar.tradingDay().format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		}
	};
	
	public abstract double resolve(Bar bar);
}