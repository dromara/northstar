package tech.quantit.northstar.strategy.api.indicator;

import tech.quantit.northstar.strategy.api.indicator.Indicator.ValueType;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * MACD指标
 * 参考文华对MACD指标的定义
 * DIFF : EMA(CLOSE,SHORT) - EMA(CLOSE,LONG);
 * DEA  : EMA(DIFF,M);
 * 2*(DIFF-DEA),COLORSTICK;
 * @author KevinHuangwl
 *
 */
public class MACD extends MultiValueIndicator{

	public static final String FAST = "FAST";
	public static final String SLOW = "SLOW";
	public static final String DEA = "DEA";
	public static final String DIFF = "DIFF";
	
	private Indicator fastLine;
	private Indicator slowLine;
	private Indicator diffLine;
	private Indicator deaLine;
	
	public MACD(String unifiedSymbol) {
		//默认参数是 (12, 26, 9)
		this(unifiedSymbol, 12, 26, 9);
	}
	
	public MACD(String unifiedSymbol, int fast, int slow, int mid) {
		fastLine = new ExpMovingAverage(unifiedSymbol, fast, ValueType.CLOSE);
		slowLine = new ExpMovingAverage(unifiedSymbol, slow, ValueType.CLOSE);
		diffLine = new ValueSeries("", fast, ValueType.NOT_SET);
		deaLine = new ExpMovingAverage("", mid, ValueType.NOT_SET);
		setIndicator(FAST, fastLine);
		setIndicator(SLOW, slowLine);
		setIndicator(DIFF, diffLine);
		setIndicator(DEA, deaLine);
	}

	@Override
	public void onBar(BarField bar) {
		fastLine.onBar(bar);
		slowLine.onBar(bar);
		diffLine.updateVal(fastLine.value(0) - slowLine.value(0), bar.getActionTimestamp());
		deaLine.updateVal(diffLine.value(0), bar.getActionTimestamp());
	}
	
}
