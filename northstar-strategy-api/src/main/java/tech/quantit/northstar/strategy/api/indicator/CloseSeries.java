package tech.quantit.northstar.strategy.api.indicator;


/**
 * 收盘价系列
 * @author KevinHuangwl
 *
 */
public class CloseSeries extends Indicator{

	protected CloseSeries(int size, ValueType valType) {
		super(size, valType);
	}

	public CloseSeries(int size) {
		this(size, ValueType.CLOSE);
	}

	@Override
	protected double updateVal(double newVal) {
		return newVal;
	}
}
