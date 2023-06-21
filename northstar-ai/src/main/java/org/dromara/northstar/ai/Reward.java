package org.dromara.northstar.ai;

public abstract class Reward {
	
	public double getValue() {
		return evaluate();
	}
	
	public abstract double evaluate();
	
}
