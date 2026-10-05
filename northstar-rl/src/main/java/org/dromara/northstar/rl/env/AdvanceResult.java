package org.dromara.northstar.rl.env;

/**
 * {@link MarketReplay#advanceBar()} 的三态返回。
 */
public enum AdvanceResult {

	/** 预热段：bar 喂给指标递推，不产生观测、不允许下单、不记录经验 */
	WARMUP,
	/** 正式段：正常推进 */
	OK,
	/** 数据耗尽：episode 正常截断（truncated），不是失败 */
	EXHAUSTED
}
