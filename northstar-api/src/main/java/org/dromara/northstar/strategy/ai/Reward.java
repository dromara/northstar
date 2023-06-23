package org.dromara.northstar.strategy.ai;

public abstract class Reward {
	
	public double getValue() {
		return evaluate();
	}
	
	public abstract double evaluate();
	
}
