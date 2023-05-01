package org.dromara.northstar.indicator.volume;

import java.util.List;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.constant.ValueType;
import org.dromara.northstar.indicator.helper.SimpleValueIndicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;

/**
 * 成交量加权平均价格（Volume Weighted Average Price）
 * @author KevinHuangwl
 *
 */
public class VWAPIndicator extends AbstractIndicator implements Indicator{

	private Indicator close;
	private Indicator volume;
	
	public VWAPIndicator(Configuration cfg, int barCount) {
		super(cfg);
		close = new SimpleValueIndicator(cfg.toBuilder().cacheLength(barCount).visible(false).build());
		volume = new SimpleValueIndicator(cfg.toBuilder().cacheLength(barCount).valueType(ValueType.VOL).visible(false).build());
	}
	
	@Override
	public List<Indicator> dependencies() {
		return List.of(close, volume);
	}

	@Override
	protected Num evaluate(Num num) {
		if(!volume.isReady()) {
			return Num.NaN();
		}
		double accVol = volume.getData().stream().mapToDouble(Num::value).sum();
		List<Num> volumes = volume.getData();
		List<Num> closes = close.getData();
		double val = 0;
		for(int i=0; i<volumes.size(); i++) {
			val += closes.get(i).value() * volumes.get(i).value();
		}
		return Num.of(val/accVol, num.timestamp(), num.unstable());
	}
	
	
}
