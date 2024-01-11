package org.dromara.northstar.indicator;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.dromara.northstar.indicator.model.RingArray;

import cn.hutool.core.lang.Assert;

/**
 * 抽象的指标
 * 代表的是一个单值指标
 * @author KevinHuangwl
 *
 */
public abstract class AbstractIndicator implements Indicator {
	
	protected RingArray<Num> ringBuf;
	
	protected Configuration cfg;
	
	protected AbstractIndicator(Configuration cfg) {
		this.cfg = cfg;
		ringBuf = new RingArray<>(cfg.cacheLength());
	}
	
	@Override
	public void update(Num num) {
		if(ringBuf.size() > 0 && (num.timestamp() < get(0).timestamp() && num.unstable() == get(0).unstable() || num.timestamp() == get(0).timestamp() && !get(0).unstable())) {
			return;	// 通过时间戳比对，确保同一个指标在同一种状态下只能被同一个时间的值更新一次
		}
		Num updateNum = evaluate(num);
		if(!updateNum.isNaN()) {
			ringBuf.update(updateNum, updateNum.unstable());
		}
	}
	
	/**
	 * 具体的指标只需要提供值更新算法
	 * @param num
	 * @return
	 */
	protected abstract Num evaluate(Num num);

	@Override
	public Num get(int step) {
		Assert.isTrue(step <= 0, "回溯步长不能是正数");
		Assert.isTrue(step > -cfg.cacheLength(), "回溯长度超过指标缓存大小");
		if(Objects.isNull(ringBuf.get(step))) {
			return Num.NaN();
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
		return ringBuf.length() == ringBuf.size();
	}

	@Override
	public List<Num> getData() {
		return Stream.of(ringBuf.toArray()).filter(Objects::nonNull).map(Num.class::cast).toList();
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
