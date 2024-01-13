package org.dromara.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class CommonUtilsTest {

	@Test
	void testLocalDateTimeToMillsConversion() {
		LocalDateTime now = LocalDateTime.now();
		long timestamp = CommonUtils.localDateTimeToMills(now);
		assertThat(CommonUtils.localDateTimeToMills(now)).isEqualTo(timestamp);
		assertThat(CommonUtils.millsToLocalDateTime(timestamp)).isEqualTo(LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), now.getHour(), now.getMinute(), now.getSecond(), (int)(timestamp % 1000 * 1000000)));
	}

	@Test
	void testIsEquals() {
		assertThat(CommonUtils.isEquals(0.1234567, 0.1234568)).isTrue();
		assertThat(CommonUtils.isEquals(0.123456, 0.123457)).isFalse();
	}

	@Test
	void testPrecision() {
		assertThat(CommonUtils.precisionOf(10)).isZero();
		assertThat(CommonUtils.precisionOf(5)).isZero();
		assertThat(CommonUtils.precisionOf(1)).isZero();
		assertThat(CommonUtils.precisionOf(0.1)).isEqualTo(1);
		assertThat(CommonUtils.precisionOf(0.2)).isEqualTo(1);
		assertThat(CommonUtils.precisionOf(0.5)).isEqualTo(1);
		assertThat(CommonUtils.precisionOf(0.05)).isEqualTo(2);
		assertThat(CommonUtils.precisionOf(0.01)).isEqualTo(2);
		assertThat(CommonUtils.precisionOf(0.0001)).isEqualTo(4);
		assertThat(CommonUtils.precisionOf(0.000000001)).isEqualTo(9);
		assertThrows(IllegalArgumentException.class, () -> {
			CommonUtils.precisionOf(0.0000000001);
		});
	}
}
