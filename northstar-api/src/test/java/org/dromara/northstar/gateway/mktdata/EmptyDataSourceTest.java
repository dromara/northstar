package org.dromara.northstar.gateway.mktdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.core.Contract;
import org.junit.jupiter.api.Test;

class EmptyDataSourceTest {
	
	@Test
	void test() {
		EmptyDataSource source = new EmptyDataSource();
		assertThat(source.getAllContracts()).isEmpty();
		assertThat(source.getMinutelyData(mock(Contract.class), LocalDate.now(), LocalDate.now())).isEmpty();
		assertThat(source.getQuarterlyData(mock(Contract.class), LocalDate.now(), LocalDate.now())).isEmpty();
		assertThat(source.getHourlyData(mock(Contract.class), LocalDate.now(), LocalDate.now())).isEmpty();
		assertThat(source.getDailyData(mock(Contract.class), LocalDate.now(), LocalDate.now())).isEmpty();
		assertThat(source.getHolidays(ChannelType.SIM, LocalDate.now(), LocalDate.now())).isEmpty();
	}

}
