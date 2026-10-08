package org.dromara.northstar.indicator.momentum;

import static org.assertj.core.api.Assertions.assertThat;

import org.dromara.northstar.common.constant.IndicatorPlacement;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.model.Configuration;
import org.junit.jupiter.api.Test;

class KDIndicatorTest {
	
	Contract c = Contract.builder().build();

	@Test
	void testGrouping() {
		KDIndicator kd = new KDIndicator(Configuration.builder().indicatorName("KD").contract(c).build(), 9, 3, 3);
		// K、D线同属一个指标组，且绘制在副图
		assertThat(kd.getK().getConfiguration().group()).isEqualTo("KD");
		assertThat(kd.getK().getConfiguration().placement()).isEqualTo(IndicatorPlacement.SUB);
		assertThat(kd.getD().getConfiguration().group()).isEqualTo("KD");
		assertThat(kd.getD().getConfiguration().placement()).isEqualTo(IndicatorPlacement.SUB);
	}

}
