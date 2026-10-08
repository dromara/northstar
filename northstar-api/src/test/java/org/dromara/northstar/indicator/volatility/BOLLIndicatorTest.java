package org.dromara.northstar.indicator.volatility;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.dromara.northstar.common.constant.IndicatorPlacement;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.Indicator;
import org.junit.jupiter.api.Test;

class BOLLIndicatorTest {
	
	Contract c = Contract.builder().build();

	@Test
	void testGrouping() {
		BOLLIndicator boll = new BOLLIndicator(c, Interval.M1, 20, 2);
		// 布林带绘制在主图，中轨、上轨、下轨同属一个指标组
		for(Indicator line : List.of(boll.getMid(), boll.getUpper(), boll.getLower())) {
			assertThat(line.getConfiguration().group()).isEqualTo("BOLL");
			assertThat(line.getConfiguration().placement()).isEqualTo(IndicatorPlacement.MAIN);
		}
	}

}
