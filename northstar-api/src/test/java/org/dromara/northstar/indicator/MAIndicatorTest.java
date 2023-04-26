package org.dromara.northstar.indicator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

class MAIndicatorTest {

	@Test
	void testMA() {
		Indicator ma5 = new MAIndicator(Configuration.builder().cacheLength(10).build(), 5);
		double[] data = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };
		for (double v : data) {
			ma5.update(Num.of(v));
		}
		assertThat(ma5.value(0).value()).isEqualTo(8.0);
		assertThat(ma5.value(-1).value()).isEqualTo(7.0);
		assertThat(ma5.value(-2).value()).isEqualTo(6.0);
	}

	@Test
	void testMAWithSmallPeriod() {
		Indicator ma2 = new MAIndicator(Configuration.builder().cacheLength(10).build(), 2);
		double[] data = { 1.0, 2.0, 3.0, 4.0, 5.0 };
		for (double v : data) {
			ma2.update(Num.of(v));
		}
		assertThat(ma2.value(0).value()).isCloseTo(4.5, offset(1e-9));
		assertThat(ma2.value(-1).value()).isCloseTo(3.5, offset(1e-9));
		assertThat(ma2.value(-2).value()).isCloseTo(2.5, offset(1e-9));
	}

	@Test
	void testMAWithVolatileValue() {
		Indicator ma5 = new MAIndicator(Configuration.builder().cacheLength(10).build(), 5);
		double[] data = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };
		for (double v : data) {
			for(int i=0; i<3; i++) {
				ma5.update(Num.of(ThreadLocalRandom.current().nextDouble(10), true)); 		// 插入不稳定值
			}
			ma5.update(Num.of(v));
		}
		assertThat(ma5.value(0).value()).isCloseTo(8.0, offset(1e-9));
		assertThat(ma5.value(-1).value()).isCloseTo(7.0, offset(1e-9));
		assertThat(ma5.value(-2).value()).isCloseTo(6.0, offset(1e-9));
	}
}
