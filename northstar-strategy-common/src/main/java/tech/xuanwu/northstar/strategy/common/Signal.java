package tech.xuanwu.northstar.strategy.common;

public interface Signal {

	boolean isOpening();
	
	boolean isBuy();
	
	boolean isSell();
	
	double price();
	
	double stopPrice();
	
}
