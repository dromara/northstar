package tech.xuanwu.northstar.strategy.common;

import tech.xuanwu.northstar.strategy.common.PriceResolver.PriceType;

public interface Signal {

	boolean isOpening();
	
	boolean isBuy();
	
	double price();
	
	double stopPrice();
	
}
