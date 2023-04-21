package org.dromara.northstar.strategy.constant;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

public enum PriceType {

	ANY_PRICE("市价"),
	
	OPP_PRICE("对手价"),
	
	LAST_PRICE("最新价"),
	
	WAITING_PRICE("排队价"),
	
	LIMIT_PRICE("限价");
	
	@Getter
	private String name;
	private PriceType(String name) {
		this.name = name;
	}
	
}
