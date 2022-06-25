package tech.quantit.northstar.strategy.api.indicator.function;

import java.util.function.DoubleUnaryOperator;

/**
 * 移动平均MA
 * @author KevinHuangwl
 *
 */
public class MovingAverage implements DoubleUnaryOperator{

	private double[] values;
	
	private int cursor;
	
	private double sumOfValues;
	
	private int size;
	
	public MovingAverage(int size) {
		this.values = new double[size];
		this.size = size;
	}

	private int nextIndex() {
		return ++cursor % size;
	}

	@Override
	public double applyAsDouble(double val) {
		double oldVal = values[cursor];
		values[cursor] = val;
		cursor = nextIndex();
		sumOfValues += val - oldVal;
		return sumOfValues / size;
	}
}
