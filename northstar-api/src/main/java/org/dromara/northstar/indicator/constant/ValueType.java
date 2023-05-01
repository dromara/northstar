package org.dromara.northstar.indicator.constant;

import xyz.redtorch.pb.CoreField.BarField;

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
		public double resolve(BarField bar) {
			return bar.getHighPrice();
		}
	},
	/**
	 * 最低价
	 */
	LOW {
		@Override
		public double resolve(BarField bar) {
			return bar.getLowPrice();
		}
	},
	/**
	 * 开盘价
	 */
	OPEN {
		@Override
		public double resolve(BarField bar) {
			return bar.getOpenPrice();
		}
	},
	/**
	 * 收盘价
	 */
	CLOSE {
		@Override
		public double resolve(BarField bar) {
			return bar.getClosePrice();
		}
	},
	/**
	 * 重心价
	 */
	BARYCENTER {
		@Override
		public double resolve(BarField bar) {
			return (bar.getHighPrice() + bar.getClosePrice() + bar.getClosePrice() * 2) / 4;
		}
	},
	/**
	 * 成交量
	 */
	VOL {
		@Override
		public double resolve(BarField bar) {
			return bar.getVolume();
		}
	},
	/**
	 * 持仓量
	 */
	OI {
		@Override
		public double resolve(BarField bar) {
			return bar.getOpenInterest();
		}
	},
	/**
	 * 持仓量变化
	 */
	OI_DELTA {
		@Override
		public double resolve(BarField bar) {
			return bar.getOpenInterestDelta();
		}
	},
	/**
	 * 交易日
	 */
	TRADE_DATE {
		@Override
		public double resolve(BarField bar) {
			return Double.valueOf(bar.getTradingDay());
		}
	};
	
	public abstract double resolve(BarField bar);
}