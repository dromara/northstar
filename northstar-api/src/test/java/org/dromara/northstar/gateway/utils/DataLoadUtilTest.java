package org.dromara.northstar.gateway.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

class DataLoadUtilTest {

	@Test
	void testSplitWeek() {
		DataLoadUtil util = new DataLoadUtil();
        List<LocalDate[]> results = new ArrayList<>();
        BiConsumer<LocalDate, LocalDate> consumer = (start, end) -> results.add(new LocalDate[]{start, end});
        util.splitByWeek(LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 31), consumer);
        
        assertThat(results).hasSize(5);
        assertThat(LocalDate.of(2024, 10, 1)).isEqualTo(results.get(0)[0]);
        assertThat(LocalDate.of(2024, 10, 6)).isEqualTo(results.get(0)[1]);
        assertThat(LocalDate.of(2024, 10, 7)).isEqualTo(results.get(1)[0]);
        assertThat(LocalDate.of(2024, 10, 13)).isEqualTo(results.get(1)[1]);
        assertThat(LocalDate.of(2024, 10, 28)).isEqualTo(results.get(4)[0]);
        assertThat(LocalDate.of(2024, 10, 31)).isEqualTo(results.get(4)[1]);
	}

	@Test
	void testSplitMonth() {
		DataLoadUtil util = new DataLoadUtil();
        List<LocalDate[]> results = new ArrayList<>();
        BiConsumer<LocalDate, LocalDate> consumer = (start, end) -> results.add(new LocalDate[]{start, end});
        util.splitByMonth(LocalDate.of(2024, 7, 10), LocalDate.of(2024, 10, 31), consumer);
        
        assertThat(results).hasSize(4);
        assertThat(LocalDate.of(2024, 7, 10)).isEqualTo(results.get(0)[0]);
        assertThat(LocalDate.of(2024, 7, 31)).isEqualTo(results.get(0)[1]);
        assertThat(LocalDate.of(2024, 10, 1)).isEqualTo(results.get(3)[0]);
        assertThat(LocalDate.of(2024, 10, 31)).isEqualTo(results.get(3)[1]);
	}
	
	@Test
	void testSplitYear() {
		DataLoadUtil util = new DataLoadUtil();
        List<LocalDate[]> results = new ArrayList<>();
        BiConsumer<LocalDate, LocalDate> consumer = (start, end) -> results.add(new LocalDate[]{start, end});
        util.splitByYear(LocalDate.of(2022, 7, 10), LocalDate.of(2024, 10, 31), consumer);
        
        assertThat(results).hasSize(3);
        assertThat(LocalDate.of(2022, 7, 10)).isEqualTo(results.get(0)[0]);
        assertThat(LocalDate.of(2022, 12, 31)).isEqualTo(results.get(0)[1]);
        assertThat(LocalDate.of(2024, 1, 1)).isEqualTo(results.get(2)[0]);
        assertThat(LocalDate.of(2024, 10, 31)).isEqualTo(results.get(2)[1]);
	}
}
