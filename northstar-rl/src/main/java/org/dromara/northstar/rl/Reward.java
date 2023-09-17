package org.dromara.northstar.rl;

public abstract class Reward {
	
	public double getValue() {
		return evaluate();
	}
	
	public abstract double evaluate();
	
}