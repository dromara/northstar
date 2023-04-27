package org.dromara.northstar.indicator;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import cn.hutool.core.lang.Assert;

public abstract class AbstractIndicator implements Indicator {
	
	protected RingArray<Num> ringBuf;
	
	protected Configuration cfg;
	
	protected AbstractIndicator(Configuration cfg) {
		this.cfg = cfg;
		ringBuf = new RingArray<>(cfg.cacheLength());
	}
	
	@Override
	public void update(Num num) {
		ringBuf.update(evaluate(num), num.unstable());
	}
	
	/**
	 * 具体的指标只需要提供值更新算法
	 * @param num
	 * @return
	 */
	protected abstract Num evaluate(Num num);

	@Override
	public Num get(int step) {
		Assert.isTrue(step <= 0, "回溯步长不是正数");
		Assert.isTrue(step > -cfg.cacheLength(), "回溯长度超过指标缓存大小");
		if(Objects.isNull(ringBuf.get(step))) {
			return Num.of(Double.NaN);
		}
		return ringBuf.get(step);
	}
	
	@Override
	public double value(int step) {
		return get(step).value();
	}

	/**
	 * 缓存填满值才算准备好
	 */
	@Override
	public boolean isReady() {
		return ringBuf.toArray().length == ringBuf.size();
	}

	@Override
	public List<Num> getData() {
		return Stream.of(ringBuf.toArray()).map(Num.class::cast).toList();
	}
	
	@Override
	public Configuration getConfiguration() {
		return cfg;
	}
	
	@Override
	public List<Indicator> dependencies() {
		return Collections.emptyList();
	}
}
