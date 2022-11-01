package tech.quantit.northstar.main.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.corundumstudio.socketio.SocketIOServer;

import tech.quantit.northstar.main.NorthstarApplication;
import tech.quantit.northstar.main.handler.broadcast.SocketIOMessageEngine;
import tech.quantit.northstar.strategy.api.utils.time.PeriodHelper;
import tech.quantit.northstar.strategy.api.utils.time.trade.PeriodHelperFactory;
import test.common.TestFieldFactory;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
class PeriodHelperFactoryTest {

	@MockBean
	private SocketIOMessageEngine msgEngine;
	
	@MockBean
	private SocketIOServer socketServer;
	
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	@Test
	void test() {
		PeriodHelper h1 = PeriodHelperFactory.newInstance(5, false, factory.makeContract("rb2301"));
		PeriodHelper h2 = PeriodHelperFactory.newInstance(5, false, factory.makeContract("rb2301"));
		PeriodHelper h4 = PeriodHelperFactory.newInstance(5, false, factory.makeContract("rb2311"));
		assertThat(h1 == h2).isTrue();
		assertThat(h1 == h4).isTrue();
		
		PeriodHelper h3 = PeriodHelperFactory.newInstance(5, true, factory.makeContract("rb2301"));
		assertThat(h1 != h3).isTrue();
	}

}
