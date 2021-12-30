package tech.quantit.northstar.strategy.api.indicator;

/**
 * 移动平均MA
 * @author KevinHuangwl
 *
 */
public class MovingAverage extends Indicator{

	private double[] values;
	
	private int cursor;
	
	private double sumOfValues;
	
	private int size;
	
	public MovingAverage(String unifiedSymbol, int size, ValueType valType) {
		super(unifiedSymbol, size, valType);
		this.values = new double[size];
		this.size = size;
	}

	@Override
	protected double updateVal(double newVal) {
		double oldVal = values[cursor];
		values[cursor] = newVal;
		cursor = nextIndex();
		sumOfValues += newVal - oldVal;
		return sumOfValues / size;
	}

	private int nextIndex() {
		return ++cursor % size;
	}
}
