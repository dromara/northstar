package tech.xuanwu.northstar.strategy.common;

public interface Signal {

	boolean isOpening();
	
	boolean isBuy();
	
	double price();
	
	double stopPrice();
	
}
