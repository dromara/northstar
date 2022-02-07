package tech.quantit.northstar.strategy.api.indicator;

import org.apache.commons.math3.stat.StatUtils;

import tech.quantit.northstar.strategy.api.indicator.Indicator.ValueType;
import xyz.redtorch.pb.CoreField;

/**
 * @author chovi.wu
 * @Description 布林k线计算算法
 * @create 2022-02-07 14:13
 */
public class Boll extends MultiValueIndicator {

    public static final String LOWER = "LOWER";
    public static final String UPPER = "UPPER";
    public static final String MID = "MID";

    private final Indicator lowerLine;

    private final Indicator upperLine;

    private final Indicator midLine;
    
    private double[] closeValues;
    
    private int cursor;
    
    private double factor;

    public Boll(String unifiedSymbol) {
        //默认参数是 (20, 2)
        this(unifiedSymbol, 20, 2);
    }

    public Boll(String unifiedSymbol, int size, double factor) {
        upperLine = new ValueSeries(unifiedSymbol, size, ValueType.NOT_SET);
        lowerLine = new ValueSeries(unifiedSymbol, size, ValueType.NOT_SET);
        midLine   = new MovingAverage(unifiedSymbol, size, ValueType.CLOSE);
        closeValues = new double[size];
        this.factor = factor;
        setIndicator(LOWER, lowerLine);
        setIndicator(UPPER, upperLine);
        setIndicator(MID, midLine);
    }

    @Override
    public void onBar(CoreField.BarField bar) {
        midLine.onBar(bar);
        closeValues[getCursorAndNext()] = bar.getClosePrice();
        double std = Math.sqrt(StatUtils.variance(closeValues, midLine.value(0)));
        upperLine.updateVal(midLine.value(0)+factor*std, bar.getActionTimestamp());
        lowerLine.updateVal(midLine.value(0)-factor*std, bar.getActionTimestamp());
    }

    
    private int getCursorAndNext() {
    	int curCursor = cursor;
    	cursor = ++cursor % closeValues.length;
    	return curCursor;
    }
}
