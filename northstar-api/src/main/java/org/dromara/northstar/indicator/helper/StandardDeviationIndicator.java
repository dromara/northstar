package org.dromara.northstar.indicator.helper;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.dromara.northstar.indicator.model.RingArray;

/**
 * 标准差指标
 * @author KevinHuangwl
 *
 */
public class StandardDeviationIndicator extends AbstractIndicator implements Indicator {

	private Indicator srcIndicator;
	
	private RingArray<Num> sample;
	
	/**
	 * 标准差指标
	 * @param cfg
	 * @param barCount		样本大小
	 */
	public StandardDeviationIndicator(Configuration cfg, int barCount) {
		super(cfg);
		this.sample = new RingArray<>(barCount);
	}
	
	/**
	 * 标准差指标
	 * @param cfg
	 * @param indicator		样本指标（指标的可回溯长度就是样本大小）
	 */
	public StandardDeviationIndicator(Configuration cfg, Indicator indicator) {
		this(cfg, 0);
		this.srcIndicator = indicator;
	}
	
	@Override
	protected Num evaluate(Num num) {
		double[] data = null;
		if(Objects.isNull(srcIndicator)) {
			sample.update(num, num.unstable());
			if(sample.toArray().length != sample.size()) {
				return Num.NaN();
			}
			data = Stream.of(sample.toArray()).map(Num.class::cast).mapToDouble(Num::value).toArray();
		} else {
			if(!srcIndicator.isReady()) {
				return Num.NaN();
			}
			data = srcIndicator.getData().stream().mapToDouble(Num::value).toArray();
		}
		
		double std = new StandardDeviation().evaluate(data);
		return Num.of(std, num.timestamp(), num.unstable());
	}

	@Override
	public List<Indicator> dependencies() {
		if(Objects.isNull(srcIndicator)) {
			return Collections.emptyList();
		}
		return List.of(srcIndicator);
	}

}
