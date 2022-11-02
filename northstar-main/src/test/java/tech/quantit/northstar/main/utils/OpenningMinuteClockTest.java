package tech.quantit.northstar.main.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.corundumstudio.socketio.SocketIOServer;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.gateway.api.domain.time.OpenningMinuteClock;
import tech.quantit.northstar.main.NorthstarApplication;
import tech.quantit.northstar.main.handler.broadcast.SocketIOMessageEngine;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.TickField;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
class OpenningMinuteClockTest {

	@MockBean
	private SocketIOMessageEngine msgEngine;
	
	@MockBean
	private SocketIOServer socketServer;
	
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	OpenningMinuteClock clock = new OpenningMinuteClock(factory.makeContract("rb2210"));

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
		
		assertThat(clock.barMinute(t1)).isEqualTo(LocalTime.of(23, 0));
		assertThat(clock.nextBarMinute()).isEqualTo(LocalTime.of(9, 1));
	}
}
