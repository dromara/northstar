package org.dromara.northstar.indicator.volatility;

import java.util.List;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Configuration;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.Num;
import org.dromara.northstar.indicator.helper.SimpleValueIndicator;
import org.dromara.northstar.strategy.constant.ValueType;

/**
 * 真实波幅指标
 * 本指标只求TR，ATR只是对TR做平均处理
 * TR : MAX(MAX((HIGH-LOW),ABS(REF(CLOSE,1)-HIGH)),ABS(REF(CLOSE,1)-LOW));//求最高价减去最低价，一个周期前的收盘价减去最高价的绝对值，一个周期前的收盘价减去最低价的绝对值，这三个值中的最大值
 * ATR : MA(TR,N);//求N个周期内的TR的简单移动平均
 * @author KevinHuangwl
 *
 */
public class TrueRangeIndicator extends AbstractIndicator implements Indicator{

	private Indicator close;
	private Indicator high;
	private Indicator low;
	
	public TrueRangeIndicator(Configuration cfg) {
		super(cfg);
		close = new SimpleValueIndicator(cfg.toBuilder().valueType(ValueType.CLOSE).visible(false).build());
		high = new SimpleValueIndicator(cfg.toBuilder().valueType(ValueType.HIGH).visible(false).build());
		low = new SimpleValueIndicator(cfg.toBuilder().valueType(ValueType.LOW).visible(false).build());
	}
	
	@Override
	public List<Indicator> dependencies() {
		return List.of(close, high, low);
	}

	@Override
	protected Num evaluate(Num num) {
		if(!isReady()) {
			return Num.NaN();
		}
		double difHighLow = high.value(0) - low.value(0);
		double difHighClose = Math.abs(high.value(0) - close.value(-1));
		double difLowClose = Math.abs(low.value(0) - close.value(-1));
		return Num.of(Math.max(difHighLow, Math.max(difHighClose, difLowClose)), num.timestamp(), num.unstable());
	}
}
