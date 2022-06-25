package tech.quantit.northstar.strategy.api.indicator.function;

import java.util.function.DoubleUnaryOperator;

/**
 * 指数加权平均EMA
 * @author KevinHuangwl
 *
 */
public class ExpMovingAverage implements DoubleUnaryOperator{

	private double ema;
	
	private double factor;
	
	private boolean hasInitVal;
	
	public ExpMovingAverage(int size) {
		this.factor = 2D / (size + 1);
	}
	
	@Override
	public double applyAsDouble(double val) {
		if(hasInitVal) {			
			ema = factor * val + (1 - factor) * ema;
		} else {
			ema = val;
			hasInitVal = true;
		}
		return ema;
	}

}
