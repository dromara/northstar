package tech.quantit.northstar.strategy.api.indicator;

/**
 * 指数加权平均EMA
 * @author KevinHuangwl
 *
 */
public class ExpMovingAverage extends Indicator{

	private double ema;
	
	private double factor;
	
	public ExpMovingAverage(int size, ValueType valType) {
		super(size, valType);
		this.factor = 2D / (size + 1);
	}

	@Override
	protected double updateVal(double newVal) {
		ema = ema > 0 ? factor * newVal + (1 - factor) * ema : newVal;
		return ema;
	}

}
