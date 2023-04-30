package org.dromara.northstar.strategy.constant;

import org.dromara.northstar.common.constant.SignalOperation;

import lombok.Getter;
import xyz.redtorch.pb.CoreField.TickField;

public enum PriceType {

	ANY_PRICE("市价") {
		@Override
		public double resolvePrice(TickField tick, SignalOperation operation, double price) {
			return 0;
		}
	},
	
	OPP_PRICE("对手价") {
		@Override
		public double resolvePrice(TickField tick, SignalOperation operation, double price) {
			return operation.isBuy() ? tick.getAskPrice(0) : tick.getBidPrice(0);
		}
	},
	
	LAST_PRICE("最新价") {
		@Override
		public double resolvePrice(TickField tick, SignalOperation operation, double price) {
			return tick.getLastPrice();
		}
	},
	
	WAITING_PRICE("排队价") {
		@Override
		public double resolvePrice(TickField tick, SignalOperation operation, double price) {
			return operation.isBuy() ? tick.getBidPrice(0) : tick.getAskPrice(0);
		}
	},
	
	LIMIT_PRICE("限价") {
		@Override
		public double resolvePrice(TickField tick, SignalOperation operation, double price) {
			return price;
		}
	};
	
	@Getter
	private String name;
	private PriceType(String name) {
		this.name = name;
	}
	
	public abstract double resolvePrice(TickField tick, SignalOperation operation, double price);
}
