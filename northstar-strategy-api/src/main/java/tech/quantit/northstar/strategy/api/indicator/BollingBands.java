package tech.quantit.northstar.strategy.api.indicator;

/**
 * @author chovi.wu
 * @Description 布林k线计算算法
 * @create 2022-02-07 15:13
 */
public class BollingBands extends Indicator {

    private double[] values;

    private int cursor;

    private double sumOfValues;

    private int size;

    private int factor;

    private int ma;

    private BollKline kline;

    public BollingBands(String unifiedSymbol, int ma, int factor, ValueType valType, BollKline bollKline) {
        super(unifiedSymbol, ma, valType);
        this.values = new double[size];
        this.size = size;
        this.ma = ma;
        this.factor = factor;
        this.kline = bollKline;
    }

    @Override
    protected double handleUpdate(double newVal) {
        values[cursor] = newVal;
        cursor = nextIndex();
        if (cursor < ma) {
            return 0d;
        }
        double pow = 0;
        double sumPow = 0;
        sumOfValues += values[cursor - ma];
        double avgClosing = sumOfValues / size;
        pow = Math.pow((newVal - avgClosing), 2);
        sumPow = sumPow + pow;
        double md = Math.sqrt(sumPow / ma);

        return switch (kline) {
            case UPPER -> (avgClosing + factor * md);
            case LOWER -> (avgClosing - factor * md);
            case MID -> (avgClosing);
            default -> throw new IllegalArgumentException("Unexpected Boll type: " + kline);
        };
    }

    private int nextIndex() {
        return ++cursor % size;
    }

    public enum BollKline {
        LOWER,
        UPPER,
        MID
    }
}
