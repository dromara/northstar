package org.dromara.northstar.strategy.constant;

import org.dromara.northstar.common.constant.SignalOperation;

import lombok.Getter;
import org.dromara.northstar.common.model.core.Tick;

public enum PriceType {

	ANY_PRICE("市价") {
		@Override
		public double resolvePrice(Tick tick, SignalOperation operation, double price) {
			return 0;
		}
	},
	
	OPP_PRICE("对手价") {
		@Override
		public double resolvePrice(Tick tick, SignalOperation operation, double price) {
			return operation.isBuy() ? tick.askPrice().get(0) : tick.bidPrice().get(0);
		}
	},
	
	LAST_PRICE("最新价") {
		@Override
		public double resolvePrice(Tick tick, SignalOperation operation, double price) {
			return tick.lastPrice();
		}
	},
	
	WAITING_PRICE("排队价") {
		@Override
		public double resolvePrice(Tick tick, SignalOperation operation, double price) {
			return operation.isBuy() ? tick.bidPrice().get(0) : tick.askPrice().get(0);
		}
	},
	
	LIMIT_PRICE("限价") {
		@Override
		public double resolvePrice(Tick tick, SignalOperation operation, double price) {
			return price;
		}
	};
	
	@Getter
	private String name;
	private PriceType(String name) {
		this.name = name;
	}
	
	public abstract double resolvePrice(Tick tick, SignalOperation operation, double price);
}
