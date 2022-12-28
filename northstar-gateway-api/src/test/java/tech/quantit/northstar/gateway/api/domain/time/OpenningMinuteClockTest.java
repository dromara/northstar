package tech.quantit.northstar.gateway.api.domain.time;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.gateway.api.domain.contract.ContractDefinition;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.TickField;

class OpenningMinuteClockTest {

	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	OpenningMinuteClock clock;
	
	@BeforeEach
	void prepare() {
		IPeriodHelperFactory phFactory = mock(IPeriodHelperFactory.class);
		when(phFactory.newInstance(anyInt(), anyBoolean(), any(ContractDefinition.class))).thenReturn(new PeriodHelper(60, mock(TradeTimeDefinition.class)));
		clock = new OpenningMinuteClock(factory.makeContract("rb2210"), new PeriodHelper(60, mock(TradeTimeDefinition.class)));
	}

	@Test
	void testBarMin() {
		TickField t1 = TickField.newBuilder()
				.setLastPrice(1000)
				.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime("100000000")
				.build();
		TickField t2 = TickField.newBuilder()
				.setLastPrice(1000)
				.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime("095900500")
				.build();
		TickField t3 = TickField.newBuilder()
				.setLastPrice(1000)
				.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime("100000500")
				.build();
		
		assertThat(clock.barMinute(t1)).isEqualTo(LocalTime.of(10, 0));
		assertThat(clock.barMinute(t2)).isEqualTo(LocalTime.of(10, 0));
		assertThat(clock.barMinute(t3)).isEqualTo(LocalTime.of(10, 1));
	}

	@Test
	void testNextMin() {
		TickField t1 = TickField.newBuilder()
				.setLastPrice(1000)
				.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime("225958000")
				.build();
		TickField t2 = TickField.newBuilder()
				.setLastPrice(1000)
				.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime("205900500")
				.build();
		assertThat(clock.barMinute(t1)).isEqualTo(LocalTime.of(23, 0));
		assertThat(clock.nextBarMinute()).isEqualTo(LocalTime.of(9, 1));
		assertThat(clock.barMinute(t2)).isEqualTo(LocalTime.of(21, 0));
	}
}
