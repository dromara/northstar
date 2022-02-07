package tech.quantit.northstar.strategy.api.indicator;

/**
 * 值序列，不做额外计算处理
 * @author KevinHuangwl
 *
 */
public class ValueSeries extends Indicator{

	public ValueSeries(String unifiedSymbol, int size, ValueType valType) {
		super(unifiedSymbol, size, valType);
	}

	@Override
	protected double handleUpdate(double newVal) {
		return newVal;
	}

}
