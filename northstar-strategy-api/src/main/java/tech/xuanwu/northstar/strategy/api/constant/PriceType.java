package tech.xuanwu.northstar.strategy.api.constant;

import lombok.Getter;

public enum PriceType {

	ANY_PRICE("市价"),
	
	OPP_PRICE("对手价"),
	
	LAST_PRICE("最新价"),
	
	WAITING_PRICE("排队价"),
	
	SIGNAL_PRICE("信号价");
	
	@Getter
	private String name;
	private PriceType(String name) {
		this.name = name;
	}
}
