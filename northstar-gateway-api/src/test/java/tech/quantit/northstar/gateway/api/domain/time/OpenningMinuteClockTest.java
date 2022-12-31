package tech.quantit.northstar.gateway.api.domain.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.TickField;

class OpenningMinuteClockTest {

	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	TradeTimeDefinition ttd = new TradeTimeDefinition() {
		
		@Override
		public List<PeriodSegment> tradeTimeSegments() {
			return List.of(new PeriodSegment(LocalTime.of(21, 0), LocalTime.of(23, 00)),
					new PeriodSegment(LocalTime.of(9, 1), LocalTime.of(10, 15)),
					new PeriodSegment(LocalTime.of(10, 31), LocalTime.of(11, 30)),
					new PeriodSegment(LocalTime.of(13, 31), LocalTime.of(15, 00)));
		}
	};
	
	OpenningMinuteClock clock;
	
	@BeforeEach
	void prepare() {
		clock = new OpenningMinuteClock(factory.makeContract("rb2210"), ttd);
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
