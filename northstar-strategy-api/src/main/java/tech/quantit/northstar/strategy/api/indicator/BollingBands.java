package tech.quantit.northstar.strategy.api.indicator;

/**
 * @author chovi.wu
 * @Description 布林k线计算算法
 * @create 2022-02-07 15:13
 */
public class BollingBands extends Indicator {

    private final double[] values;

    private int cursor;

    private double sumOfValues;

    private final int factor;

    private final int ma;

    private boolean gtMa;

    private final BollKline kline;

    public BollingBands(String unifiedSymbol, int ma, int factor, ValueType valType, BollKline bollKline) {
        super(unifiedSymbol, ma, valType);
        this.values = new double[ma];
        this.ma = ma;
        this.factor = factor;
        this.kline = bollKline;
    }
    /**
     * 计算最新k线的BOLL值的方法
     * @return 日BOLL指标的计算过程
     * 1）计算MA ---> MA=N日内的收盘价之和÷N
     * 2）计算标准差MD ----> MD=平方根N日的（C－MA）的两次方之和除以N
     * 3）计算MB、UP、DN线 --->  MB=N日的MA UP=MB＋2×MD DN=MB－2×MD
     */
    @Override
    protected double handleUpdate(double newVal) {
        double oldVal = values[cursor];
        values[cursor] = newVal;
        cursor = nextIndex();
        sumOfValues += newVal;
        if (!gtMa) {
            return 0d;
        }
        double sumPow = 0;
        sumOfValues -= oldVal;
        double avgClosing = sumOfValues / ma;
        double pow = Math.pow((newVal - avgClosing), 2);
        sumPow = sumPow + pow;
        double md = Math.sqrt(sumPow / ma);

        return switch (this.kline) {
            case UPPER -> (avgClosing + factor * md);
            case LOWER -> (avgClosing - factor * md);
            case MID -> (avgClosing);
        };
    }

    private int nextIndex() {
        this.cursor = ++cursor % ma;
        if (!gtMa && this.cursor % ma == 0) {
            gtMa = true;
        }
        return cursor;
    }

    public enum BollKline {
        LOWER,
        UPPER,
        MID
    }
}
