package org.dromara.northstar.indicator.trend;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.springframework.util.Assert;

/**
 * SMA扩展指数加权移动平均线
 * @author KevinHuangwl
 *
 */
public class SMAIndicator extends AbstractIndicator implements Indicator{

	private double factor;
	
	private Indicator srcIndicator;
	
	public SMAIndicator(Configuration cfg, int barCount, int weight) {
		super(cfg);
		Assert.isTrue(barCount > weight, "更新权重设置异常");
		this.factor = (double) weight / barCount;
	}
	
	public SMAIndicator(Configuration cfg, Indicator srcIndicator, int barCount, int weight) {
		this(cfg, barCount, weight);
		this.srcIndicator = srcIndicator;
	}

	@Override
	public List<Indicator> dependencies() {
		if(Objects.isNull(srcIndicator)) {
			return Collections.emptyList();
		}
		return List.of(srcIndicator);
	}
	
	@Override
	protected Num evaluate(Num num) {
		Num newVal = Objects.isNull(srcIndicator) ? num : srcIndicator.get(0);
		if(newVal.isNaN() || ringBuf.size() == 0 || ringBuf.size() == 1 && ringBuf.get().unstable()) {
			// 当计算样本没有值，或只有一个不稳定值时
			return newVal;
		}
		Num preVal = ringBuf.get().unstable() ? get(-1) : get(0); 
		if(preVal.isNaN()) {
			return newVal;
		}
		double val = factor * newVal.value() + (1 - factor) * preVal.value();
		return Num.of(val, num.timestamp(), num.unstable());
	}

}
