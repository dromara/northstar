package org.dromara.northstar.indicator.trend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.util.concurrent.ThreadLocalRandom;

import org.dromara.northstar.indicator.Configuration;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.Num;
import org.junit.jupiter.api.Test;

class MAIndicatorTest {

	@Test
	void testMA() {
		Indicator ma5 = new MAIndicator(Configuration.builder().cacheLength(10).build(), 5);
		double[] data = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };
		long t = 0L;
		for (double v : data) {
			ma5.update(Num.of(v, t++));
		}
		assertThat(ma5.value(0)).isEqualTo(8.0);
		assertThat(ma5.value(-1)).isEqualTo(7.0);
		assertThat(ma5.value(-2)).isEqualTo(6.0);
	}

	@Test
	void testMAWithSmallPeriod() {
		Indicator ma2 = new MAIndicator(Configuration.builder().cacheLength(10).build(), 2);
		double[] data = { 1.0, 2.0, 3.0, 4.0, 5.0 };
		long t = 0L;
		for (double v : data) {
			ma2.update(Num.of(v, t++));
		}
		assertThat(ma2.value(0)).isCloseTo(4.5, offset(1e-9));
		assertThat(ma2.value(-1)).isCloseTo(3.5, offset(1e-9));
		assertThat(ma2.value(-2)).isCloseTo(2.5, offset(1e-9));
	}

	@Test
	void testMAWithVolatileValue() {
		Indicator ma5 = new MAIndicator(Configuration.builder().cacheLength(10).build(), 5);
		double[] data = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };
		long t = 0L;
		for (double v : data) {
			for(int i=0; i<3; i++) {
				ma5.update(Num.of(ThreadLocalRandom.current().nextDouble(10), t++, true)); 		// 插入不稳定值
			}
			ma5.update(Num.of(v, t++));
		}
		assertThat(ma5.value(0)).isCloseTo(8.0, offset(1e-9));
		assertThat(ma5.value(-1)).isCloseTo(7.0, offset(1e-9));
		assertThat(ma5.value(-2)).isCloseTo(6.0, offset(1e-9));
	}
}
