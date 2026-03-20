package org.dromara.northstar.gateway.mktdata;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.junit.jupiter.api.Test;

class DataSourceDataLoaderTest {
	
	IDataSource ds = mock(IDataSource.class);
	
	DataSourceDataLoader loader = new DataSourceDataLoader(ds);

	Contract c = Contract.builder()
			.unifiedSymbol("rb0000@SHFE@FUTURES")
			.build();
	
	@SuppressWarnings("unchecked")
	@Test
	void testLoadMinutelyData() {
		LocalDate startDate = LocalDate.of(2024, 3, 1);
		LocalDate endDate = LocalDate.of(2024, 3, 22);
		Consumer<List<Bar>> cb = mock(Consumer.class);
		loader.loadMinutelyData(c, startDate, endDate, cb);
		verify(cb, times(4)).accept(any());
		verify(ds).getMinutelyData(any(Contract.class), eq(LocalDate.of(2024, 3, 1)), eq(LocalDate.of(2024, 3, 3)));
		verify(ds).getMinutelyData(any(Contract.class), eq(LocalDate.of(2024, 3, 4)), eq(LocalDate.of(2024, 3, 10)));
		verify(ds).getMinutelyData(any(Contract.class), eq(LocalDate.of(2024, 3, 11)), eq(LocalDate.of(2024, 3, 17)));
		verify(ds).getMinutelyData(any(Contract.class), eq(LocalDate.of(2024, 3, 18)), eq(LocalDate.of(2024, 3, 22)));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testLoadQuarterlyData() {
		LocalDate startDate = LocalDate.of(2024, 3, 11);
		LocalDate endDate = LocalDate.of(2024, 5, 22);
		Consumer<List<Bar>> cb = mock(Consumer.class);
		loader.loadQuarterlyData(c, startDate, endDate, cb);
		verify(cb, times(3)).accept(any());
		verify(ds).getQuarterlyData(any(Contract.class), eq(LocalDate.of(2024, 3, 11)), eq(LocalDate.of(2024, 3, 31)));
		verify(ds).getQuarterlyData(any(Contract.class), eq(LocalDate.of(2024, 4, 1)), eq(LocalDate.of(2024, 4, 30)));
		verify(ds).getQuarterlyData(any(Contract.class), eq(LocalDate.of(2024, 5, 1)), eq(LocalDate.of(2024, 5, 22)));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testLoadHourlyData() {
		LocalDate startDate = LocalDate.of(2024, 2, 11);
		LocalDate endDate = LocalDate.of(2024, 5, 22);
		Consumer<List<Bar>> cb = mock(Consumer.class);
		loader.loadHourlyData(c, startDate, endDate, cb);
		verify(cb, times(4)).accept(any());
		verify(ds).getHourlyData(any(Contract.class), eq(LocalDate.of(2024, 2, 11)), eq(LocalDate.of(2024, 2, 29)));
		verify(ds).getHourlyData(any(Contract.class), eq(LocalDate.of(2024, 3, 1)), eq(LocalDate.of(2024, 3, 31)));
		verify(ds).getHourlyData(any(Contract.class), eq(LocalDate.of(2024, 4, 1)), eq(LocalDate.of(2024, 4, 30)));
		verify(ds).getHourlyData(any(Contract.class), eq(LocalDate.of(2024, 5, 1)), eq(LocalDate.of(2024, 5, 22)));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testLoadDailyData() {
		LocalDate startDate = LocalDate.of(2024, 2, 11);
		LocalDate endDate = LocalDate.of(2025, 5, 22);
		Consumer<List<Bar>> cb = mock(Consumer.class);
		loader.loadDailyData(c, startDate, endDate, cb);
		verify(cb, times(2)).accept(any());
		verify(ds).getDailyData(any(Contract.class), eq(LocalDate.of(2024, 2, 11)), eq(LocalDate.of(2024, 12, 31)));
		verify(ds).getDailyData(any(Contract.class), eq(LocalDate.of(2025, 1, 1)), eq(LocalDate.of(2025, 5, 22)));
	}

}
