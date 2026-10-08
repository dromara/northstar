package org.dromara.northstar.indicator.trend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.dromara.northstar.common.constant.IndicatorPlacement;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.LineStyle;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.junit.jupiter.api.Test;

class MACDIndicatorTest {
	
	Contract c = Contract.builder().build();

	@Test
	void testGrouping() {
		MACDIndicator macd = new MACDIndicator(c, Interval.M1, 12, 26, 9);
		// 本体与diff、dea线同属一个指标组，且绘制在副图
		assertThat(macd.getConfiguration().group()).isEqualTo("MACD");
		assertThat(macd.getConfiguration().placement()).isEqualTo(IndicatorPlacement.SUB);
		assertThat(macd.getConfiguration().lineStyle()).isEqualTo(LineStyle.ZERO_BASED_BARS);
		assertThat(macd.getDiffLine().getConfiguration().group()).isEqualTo("MACD");
		assertThat(macd.getDiffLine().getConfiguration().placement()).isEqualTo(IndicatorPlacement.SUB);
		assertThat(macd.getDiffLine().getConfiguration().lineStyle()).isEqualTo(LineStyle.LINES);
		assertThat(macd.getDeaLine().getConfiguration().group()).isEqualTo("MACD");
		assertThat(macd.getDeaLine().getConfiguration().placement()).isEqualTo(IndicatorPlacement.SUB);
		assertThat(macd.getDeaLine().getConfiguration().lineStyle()).isEqualTo(LineStyle.LINES);
		// 快慢线仅参与计算，不显示
		List<Indicator> fastSlowLines = macd.getDiffLine().dependencies();
		assertThat(fastSlowLines).hasSize(2);
		assertThat(fastSlowLines).allMatch(line -> !line.getConfiguration().visible());
		assertThat(fastSlowLines).extracting(line -> line.getConfiguration().indicatorName())
				.containsExactlyInAnyOrder("MACD_fast", "MACD_slow");
	}
	
	@Test
	void testGroupingWithCustomFastSlowLines() {
		Indicator fastLine = new EMAIndicator(Configuration.builder().indicatorName("fast").contract(c).build(), 12);
		Indicator slowLine = new EMAIndicator(Configuration.builder().indicatorName("slow").contract(c).build(), 26);
		MACDIndicator macd = new MACDIndicator(Configuration.builder().indicatorName("MACD2").contract(c).build(), fastLine, slowLine, 9);
		assertThat(macd.getConfiguration().group()).isEqualTo("MACD2");
		assertThat(macd.getConfiguration().placement()).isEqualTo(IndicatorPlacement.SUB);
		assertThat(macd.getConfiguration().lineStyle()).isEqualTo(LineStyle.ZERO_BASED_BARS);
		assertThat(macd.getDiffLine().getConfiguration().group()).isEqualTo("MACD2");
		assertThat(macd.getDiffLine().getConfiguration().placement()).isEqualTo(IndicatorPlacement.SUB);
		assertThat(macd.getDeaLine().getConfiguration().group()).isEqualTo("MACD2");
		assertThat(macd.getDeaLine().getConfiguration().placement()).isEqualTo(IndicatorPlacement.SUB);
	}

}
