package org.dromara.northstar.indicator.trend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.dromara.northstar.common.constant.IndicatorPlacement;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.Indicator;
import org.junit.jupiter.api.Test;

class PBXIndicatorTest {
	
	Contract c = Contract.builder().build();

	@Test
	void testGrouping() {
		PBXIndicator pbx = new PBXIndicator(c, Interval.M1);
		// 瀑布线绘制在主图，六条线同属一个指标组
		List<Indicator> lines = List.of(pbx.getPb1(), pbx.getPb2(), pbx.getPb3(), pbx.getPb4(), pbx.getPb5(), pbx.getPb6());
		assertThat(lines).allSatisfy(line -> {
			assertThat(line.getConfiguration().group()).isEqualTo("PBX");
			assertThat(line.getConfiguration().placement()).isEqualTo(IndicatorPlacement.MAIN);
		});
	}

}
