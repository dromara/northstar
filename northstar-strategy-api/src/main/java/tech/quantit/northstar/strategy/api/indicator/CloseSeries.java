package tech.quantit.northstar.strategy.api.indicator;


/**
 * 收盘价系列
 * @author KevinHuangwl
 *
 */
public class CloseSeries extends ValueSeries{

	protected CloseSeries(String unifiedSymbol, int size, ValueType valType) {
		super(unifiedSymbol, size, valType);
	}

	public CloseSeries(String unifiedSymbol, int size) {
		this(unifiedSymbol, size, ValueType.CLOSE);
	}

}
