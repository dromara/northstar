package tech.xuanwu.northstar.strategy.common.model.data;

public class SimpleBar {

	protected double open;
	
	protected double high;
	
	protected double low;
	
	protected double close;
	
	public SimpleBar(double open) {
		this.open = open;
		this.high = open;
		this.low = open;
		this.close = open;
	}
	
	public void update(double val) {
		close = val;
		high = Math.max(high, val);
		low = Math.min(low, val);
	}
	
	public double barRange() {
		return high - low;
	}
	
	public double upperShadow() {
		return high - Math.max(open, close);
	}
	
	public double lowerShadow() {
		return Math.min(open, close) - low;
	}
	
	public boolean isPositive() {
		return close > open;
	}
}
