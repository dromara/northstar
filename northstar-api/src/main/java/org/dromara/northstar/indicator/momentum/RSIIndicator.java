package org.dromara.northstar.indicator.momentum;

import java.util.List;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Configuration;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.Num;
import org.dromara.northstar.indicator.helper.SimpleValueIndicator;
import org.dromara.northstar.indicator.trend.SMAIndicator;
import org.dromara.northstar.strategy.constant.ValueType;

/**
 * RSI指标，本质上是一个单值指标
 * LC := REF(CLOSE,1);//前一周期收盘价
 * RSI1:SMA(MAX(CLOSE-LC,0),N1,1)/SMA(ABS(CLOSE-LC),N1,1)*100;//当根K线收盘价与前一周期收盘价做差，在该差值与0之间取最大值，做N1周期移动平均。收盘价与前一周期收盘价做差值，取该差值的N1周期移动平均值，两平均值之间做比值。
 * RSI2:SMA(MAX(CLOSE-LC,0),N2,1)/SMA(ABS(CLOSE-LC),N2,1)*100;//当根K线收盘价与前一周期收盘价做差，在该差值与0之间取最大值，做N2周期移动平均。收盘价与前一周期收盘价做差值，取该差值的N2周期移动平均值，两平均值之间做比值。
 * @author KevinHuangwl
 *
 */
public class RSIIndicator extends AbstractIndicator implements Indicator {

	private Indicator close;
	private Indicator numerator;	//分子
	private Indicator denominator;	//分母
	
	public RSIIndicator(Configuration cfg, int barCount) {
		super(cfg);
		close = new SimpleValueIndicator(cfg.toBuilder().valueType(ValueType.CLOSE).visible(false).build());
		numerator = new SMAIndicator(cfg.toBuilder().visible(false).build(), barCount, 1);
		denominator = new SMAIndicator(cfg.toBuilder().visible(false).build(), barCount, 1);
	}
	
	@Override
	public List<Indicator> dependencies() {
		return List.of(close);
	}

	@Override
	protected Num evaluate(Num num) {
		if(!close.isReady()) {
			return Num.NaN();
		}
		Num numeratorUpdateVal = Num.of(Math.max(close.value(0) - close.value(-1), 0), num.timestamp(), num.unstable()); 
		Num denominatorUpdateVal = Num.of(Math.abs(close.value(0) - close.value(-1)), num.timestamp(), num.unstable());
		numerator.update(numeratorUpdateVal);
		denominator.update(denominatorUpdateVal);
		double val = numerator.value(0) / denominator.value(0) * 100;
		return Num.of(val, num.timestamp(), num.unstable());
	}
	
}
