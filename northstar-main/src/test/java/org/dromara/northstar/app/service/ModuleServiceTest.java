package org.dromara.northstar.app.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * ModuleService 口径单测：模组运行页的盈亏比计算。
 */
class ModuleServiceTest {

	@Test
	void profitLossRatioFollowsNullConvention() {
		assertNull(ModuleService.profitLossRatio(150, 0), "无亏损（含全胜与零交易）时盈亏比为 null");
		assertEquals(1.5, ModuleService.profitLossRatio(150, 100), 1e-9, "平均盈亏比 = 平均盈利 / 平均亏损绝对值");
		assertEquals(0.0, ModuleService.profitLossRatio(0, 100), 1e-9, "全亏时盈亏比为 0");
	}
}
