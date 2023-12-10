package org.dromara.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class DateTimeUtilsTest {

	@Test
	void testTimeCache() {
		LocalTime t1 = LocalTime.of(1, 13);
    	LocalTime t2 = LocalTime.of(1, 13);
    	assertThat(t1).isNotSameAs(t2);
    	
    	LocalTime t3 = DateTimeUtils.fromCacheTime(t1);
    	LocalTime t4 = DateTimeUtils.fromCacheTime(t2);
    	assertThat(t3).isSameAs(t4);
	}

}
