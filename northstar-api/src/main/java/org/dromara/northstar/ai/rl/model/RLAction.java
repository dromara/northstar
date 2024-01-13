package org.dromara.northstar.ai.rl.model;

import cn.hutool.core.lang.Assert;

/**
 * 强化学习马可夫过程中的行动
 * 在交易场景中，规定行动的取值空间为[-3, 3]
 * 其中，正数代表多头持仓，负数代表空头持仓，零代表无持仓。
 * 考虑到可能存在的加减仓场景，因此定义了3个级别的强度，来表示对应的持仓比例，绝对值越大代表持仓比例越大；
 * @auth KevinHuangwl
 */
public record RLAction(int value) {

	public RLAction{
		Assert.isTrue(-3 <= value && value <= 3, "不接受非法行动值");
	}
}
