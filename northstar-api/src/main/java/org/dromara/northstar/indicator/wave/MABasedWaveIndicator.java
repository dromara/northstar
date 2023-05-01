package org.dromara.northstar.indicator.wave;

import java.util.List;
import java.util.Objects;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.constant.ValueType;
import org.dromara.northstar.indicator.helper.SimpleValueIndicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.springframework.util.Assert;

/**
 * 波浪指标
 * 
 * @author KevinHuangwl
 *
 */
public class MABasedWaveIndicator extends AbstractIndicator implements Indicator {

	private int numOfBarToConfirmTheSegment;
	private EndpointType type;

	private Boolean isGoingUp;

	private Double sectionMax;
	private Double sectionMin;

	private Indicator high;
	private Indicator low;
	private Indicator maLine;
	private Indicator close;

	public MABasedWaveIndicator(Configuration cfg, Indicator maLine, int numOfBarToConfirmTheSegment,
			EndpointType type) {
		super(cfg);
		this.numOfBarToConfirmTheSegment = numOfBarToConfirmTheSegment;
		this.type = type;
		this.maLine = maLine;
		this.close = new SimpleValueIndicator(cfg.toBuilder().valueType(ValueType.CLOSE).visible(false).build());
		this.high = new SimpleValueIndicator(cfg.toBuilder().valueType(ValueType.HIGH).visible(false).build());
		this.low = new SimpleValueIndicator(cfg.toBuilder().valueType(ValueType.LOW).visible(false).build());
		Assert.isTrue(maLine.getConfiguration().cacheLength() > numOfBarToConfirmTheSegment, "可回溯长度不足以确定波浪");
		Assert.isTrue(cfg.cacheLength() > numOfBarToConfirmTheSegment, "可回溯长度不足以确定波浪");
	}

	@Override
	public List<Indicator> dependencies() {
		return List.of(close, high, low, maLine);
	}

	@Override
	protected Num evaluate(Num num) {
		if (!maLine.isReady()) {
			return Num.NaN();
		}
		// 各关键值的初始化
		if (Objects.isNull(isGoingUp)) 
			isGoingUp = close.value(0) > maLine.value(0);
		if (Objects.isNull(sectionMax)) 
			sectionMax = type == EndpointType.CLOSE ? close.value(0) : high.value(0);
		if (Objects.isNull(sectionMin)) 
			sectionMin = type == EndpointType.CLOSE ? close.value(0) : low.value(0);

		// 记录波段最大值
		if (isGoingUp) {
			sectionMax = type == EndpointType.CLOSE 
					? Math.max(sectionMax, close.value(0))
					: Math.max(sectionMax, high.value(0));
		} else {
			sectionMin = type == EndpointType.CLOSE 
					? Math.min(sectionMin, close.value(0))
					: Math.min(sectionMin, low.value(0));
		}

		if (isGoingUp && close.value(-numOfBarToConfirmTheSegment) > maLine.value(-numOfBarToConfirmTheSegment)) {
			boolean isValidTurnAround = true;
			for (int i = 0; i < numOfBarToConfirmTheSegment; i++) {
				if (close.value(-i) > maLine.value(-i)) {
					isValidTurnAround = false;
					break;
				}
			}
			if(isValidTurnAround) {
				isGoingUp = false;
				return Num.of(sectionMax, num.timestamp(), num.unstable());
			}
		}
		

		if (!isGoingUp && close.value(-numOfBarToConfirmTheSegment) < maLine.value(-numOfBarToConfirmTheSegment)) {
			boolean isValidTurnAround = true;
			for (int i = 0; i < numOfBarToConfirmTheSegment; i++) {
				if (close.value(-i) < maLine.value(-i)) {
					isValidTurnAround = false;
					break;
				}
			}
			if(isValidTurnAround) {
				isGoingUp = true;
				return Num.of(sectionMin, num.timestamp(), num.unstable());
			}
		}

		return Num.NaN();
	}

	public enum EndpointType {
		CLOSE, HIGH_LOW;
	}
}
