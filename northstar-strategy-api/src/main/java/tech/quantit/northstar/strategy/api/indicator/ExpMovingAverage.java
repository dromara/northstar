package tech.quantit.northstar.strategy.api.indicator;

/**
 * 指数加权平均EMA
 * @author KevinHuangwl
 *
 */
public class ExpMovingAverage extends Indicator{

	private double ema;
	
	private double factor;
	
	private boolean hasInitVal;
	
	public ExpMovingAverage(String unifiedSymbol, int size, ValueType valType) {
		super(unifiedSymbol, size, valType);
		this.factor = 2D / (size + 1);
	}

	@Override
	protected double handleUpdate(double newVal) {
		if(hasInitVal) {			
			ema = factor * newVal + (1 - factor) * ema;
		} else {
			ema = newVal;
			hasInitVal = true;
		}
		return ema;
	}

}
