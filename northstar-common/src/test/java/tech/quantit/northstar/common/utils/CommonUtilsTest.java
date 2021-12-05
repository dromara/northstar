package tech.quantit.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

public class CommonUtilsTest {


	@Test
	public void testLocalDateTimeToMillsConversion() {
		LocalDateTime now = LocalDateTime.now();
		long timestamp = CommonUtils.localDateTimeToMills(now);
		assertThat(CommonUtils.localDateTimeToMills(now)).isEqualTo(timestamp);
		assertThat(CommonUtils.millsToLocalDateTime(timestamp)).isEqualTo(LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), now.getHour(), now.getMinute(), now.getSecond(), (int)(timestamp % 1000 * 1000000)));
	}

	@Test
	public void testIsEquals() {
		assertThat(CommonUtils.isEquals(0.1234567, 0.1234568)).isTrue();
		assertThat(CommonUtils.isEquals(0.123456, 0.123457)).isFalse();
	}

}
