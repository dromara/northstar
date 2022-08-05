package tech.quantit.northstar.strategy.api.indicator.complex;

import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;

import static tech.quantit.northstar.strategy.api.indicator.function.AverageFunctions.*;

public final class BOLL {

    private int x;
    private int n;

    /**
     * 创建一个布林线生成器
     * @param n		代表均线周期
     * @param x		代表标准差倍数
     */
    public BOLL(int n, int x) {
        this.x = x;
        this.n = n;
    }

    /**
     * 创建一个布林线生成器
     * @param n		代表均线周期
     * @param x		代表标准差倍数
     * @return
     */
    public static BOLL of(int n, int x){
        return new BOLL(n, x);
    }

    /**
     * 获取上轨线生成函数
     * @return
     */
    public TimeSeriesUnaryOperator upper(){
    	final TimeSeriesUnaryOperator ma = MA(n);
    	final TimeSeriesUnaryOperator std = STD(n);
        return tv -> {
            TimeSeriesValue v = ma.apply(tv);
            TimeSeriesValue v0 = std.apply(tv);
            v.setValue(v.getValue() + x * v0.getValue());
            return v;
        };
    }

    /**
     * 获取下轨线生成函数
     * @return
     */
    public TimeSeriesUnaryOperator lower(){
    	final TimeSeriesUnaryOperator ma = MA(n);
    	final TimeSeriesUnaryOperator std = STD(n);
        return tv -> {
            TimeSeriesValue v = ma.apply(tv);
            TimeSeriesValue v0 = std.apply(tv);
            v.setValue(v.getValue() - x * v0.getValue());
            return v;
        };
    }

    /**
     * 获取中轨线生成函数
     * @return
     */
    public TimeSeriesUnaryOperator mid(){
        return MA(n);
    }

}
