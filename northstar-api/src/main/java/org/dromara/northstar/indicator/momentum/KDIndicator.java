package org.dromara.northstar.indicator.momentum;

import java.util.List;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Configuration;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.Num;
import org.dromara.northstar.indicator.helper.HHVIndicator;
import org.dromara.northstar.indicator.helper.LLVIndicator;
import org.dromara.northstar.indicator.helper.SimpleValueIndicator;
import org.dromara.northstar.indicator.trend.SMAIndicator;
import org.dromara.northstar.strategy.constant.ValueType;

/**
 * KD指标
 * N,M1,M2为KD指标参数
 * RSV:=(CLOSE-LLV(LOW,N))/(HHV(HIGH,N)-LLV(LOW,N))*100;//收盘价与N周期最低值做差，N周期最高值与N周期最低值做差，两差之间做比值。
 * K:SMA(RSV,M1,1);//RSV的M1日移动平均值，1为权重
 * D:SMA(K,M2,1);//K的M2日移动平均值，1为权重
 * @author KevinHuangwl
 *
 */
public class KDIndicator extends AbstractIndicator implements Indicator{

	private static String ERR_MSG = "KD指标是一个多值指标，不能直接调用该指标方法，应该获取具有的指标线进行调用";
	
	private Indicator rsv;
	private Indicator k;
	private Indicator d;
	
	public KDIndicator(Configuration cfg, int barCount, int m1, int m2) {
		super(cfg.toBuilder().visible(false).build());
		rsv = new RSVIndicator(cfg.toBuilder().visible(false).build(), barCount);
		k = new SMAIndicator(cfg.toBuilder().indicatorName(cfg.indicatorName() + "_k").build(), rsv, m1, 1);
		d = new SMAIndicator(cfg.toBuilder().indicatorName(cfg.indicatorName() + "_d").build(), k, m2, 1);
	}

	@Override
	protected Num evaluate(Num num) {
		return num;
	}
	
	@Override
	public Num get(int step) {
		throw new UnsupportedOperationException(ERR_MSG);
	}

	@Override
	public double value(int step) {
		throw new UnsupportedOperationException(ERR_MSG);
	}

	@Override
	public List<Num> getData() {
		throw new UnsupportedOperationException(ERR_MSG);
	}
	
	@Override
	public List<Indicator> dependencies() {
		return List.of(d);
	}

	public Indicator getK() {
		return k;
	}
	
	public Indicator getD() {
		return d;
	}
	
	public class RSVIndicator extends AbstractIndicator implements Indicator {
		
		private Indicator close;
		private Indicator high;
		private Indicator low;
		private Indicator llv;
		private Indicator hhv;
		
		public RSVIndicator(Configuration cfg, int barCount) {
			super(cfg);
			close = new SimpleValueIndicator(cfg.toBuilder().valueType(ValueType.CLOSE).visible(false).build());
			high = new SimpleValueIndicator(cfg.toBuilder().cacheLength(barCount).valueType(ValueType.HIGH).visible(false).build());
			low = new SimpleValueIndicator(cfg.toBuilder().cacheLength(barCount).valueType(ValueType.LOW).visible(false).build());
			llv = new LLVIndicator(cfg.toBuilder().visible(false).build(), low);
			hhv = new HHVIndicator(cfg.toBuilder().visible(false).build(), high);
		}
		
		@Override
		public List<Indicator> dependencies() {
			return List.of(close, llv, hhv);
		}

		@Override
		protected Num evaluate(Num num) {
			double val = (close.value(0) - llv.value(0)) / (hhv.value(0) - llv.value(0)) * 100;
			return Num.of(val, num.timestamp(), num.unstable()) ;
		}

	}
}
