package org.dromara.northstar.indicator;

/**
 * 抽象的均值指标
 * @author KevinHuangwl
 *
 */
public abstract class AbstractMAIndicator extends AbstractIndicator {
	
	protected RingArray<Num> sample;
	
	protected int barCount;

	protected AbstractMAIndicator(Configuration cfg, int barCount) {
		super(cfg);
		this.barCount = barCount;
		this.sample = new RingArray<>(barCount);
	}
	
	@Override
	public void update(Num num) {
		Num val = evaluate(num);
		sample.update(val, num.unstable());
		super.update(val);
	}
	
	/**
	 * 不同的均值算法只需要给出具体算法
	 * @param num
	 * @return
	 */
	protected abstract Num evaluate(Num num);
	
	@Override
	public boolean isReady() {
		return sample.toArray().length == sample.size() && super.isReady();
	}

}
