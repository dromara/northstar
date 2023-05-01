package org.dromara.northstar.indicator.volume;

import java.util.ArrayList;
import java.util.List;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Configuration;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.Num;
import org.dromara.northstar.indicator.PeriodUnit;
import org.dromara.northstar.indicator.ValueType;
import org.dromara.northstar.indicator.helper.SimpleValueIndicator;

/**
 * 日内成交量加权均价，近似于SETTLE
 * 按1分钟K线计算 
 * @author KevinHuangwl
 *
 */
public class IntraDaySettlePriceIndicator extends AbstractIndicator implements Indicator{

	private Indicator close;
	private Indicator volume;
	
	private List<Num> vols = new ArrayList<>();
	
	private long tradingDayIntFormat;
	
	public IntraDaySettlePriceIndicator(Configuration cfg) {
		super(cfg.toBuilder().numOfUnits(1).period(PeriodUnit.MINUTE).valueType(ValueType.TRADE_DATE).build());		// 强制改变外部传入的配置
		close = new SimpleValueIndicator(Configuration.builder().contract(cfg.contract()).valueType(ValueType.CLOSE).visible(false).build());
		volume = new SimpleValueIndicator(Configuration.builder().contract(cfg.contract()).valueType(ValueType.VOL).visible(false).build());
	}
	
	@Override
	public List<Indicator> dependencies() {
		return List.of(close, volume);
	}

	@Override
	protected Num evaluate(Num num)	// 这里的入参是个日期值: Num.of(Double.valueOf(dayString))
	{					
		long curTradeDate = Math.round(num.value());
		if(tradingDayIntFormat != curTradeDate) {
			tradingDayIntFormat = curTradeDate;
			vols.clear();
		}
		if(vols.isEmpty() || !get(0).unstable()) {
			vols.add(volume.get(0));
		} else {
			vols.set(vols.size() - 1, volume.get(0));
		}
		double accVol = vols.stream().mapToDouble(Num::value).sum();
		double preVal = vols.size() == 1 ? 0 : get(-1).value();
		double factor = volume.value(0) / accVol;
		double val = factor * close.value(0) + (1 - factor) * preVal;
		return Num.of(val, num.timestamp(), num.unstable());			// 输出的是加权均价值
	}
	
}
