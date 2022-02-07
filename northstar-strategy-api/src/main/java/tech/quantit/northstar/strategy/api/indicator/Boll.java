package tech.quantit.northstar.strategy.api.indicator;

import tech.quantit.northstar.strategy.api.indicator.BollingBands.BollKline;
import xyz.redtorch.pb.CoreField;

/**
 * @author chovi.wu
 * @Description boll kline
 * @create 2022-02-07 14:13
 */
public class Boll extends MultiValueIndicator {

    public static final String LOWER = "LOWER";
    public static final String UPPER = "UPPER";
    public static final String MID = "MID";

    private final Indicator lowerLine;

    private final Indicator upperLine;

    private final Indicator midLine;

    public Boll(String unifiedSymbol) {
        //默认参数是 (20, 2)
        this(unifiedSymbol, 20, 2);
    }

    public Boll(String unifiedSymbol, int ma, int factor) {
        upperLine = new BollingBands(unifiedSymbol, ma, factor, Indicator.ValueType.CLOSE, BollKline.UPPER);
        lowerLine = new BollingBands(unifiedSymbol, ma, factor, Indicator.ValueType.CLOSE, BollKline.LOWER);
        midLine   = new BollingBands(unifiedSymbol, ma, factor, Indicator.ValueType.CLOSE, BollKline.MID);
        setIndicator(LOWER, lowerLine);
        setIndicator(UPPER, upperLine);
        setIndicator(MID, midLine);
    }

    @Override
    public void onBar(CoreField.BarField bar) {
        upperLine.onBar(bar);
        lowerLine.onBar(bar);
        midLine.onBar(bar);
    }

}
