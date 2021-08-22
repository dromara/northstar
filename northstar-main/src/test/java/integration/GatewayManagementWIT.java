package integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.corundumstudio.socketio.SocketIOServer;

import common.TestGatewayFactory;
import common.TestMongoUtils;
import tech.xuanwu.northstar.NorthstarApplication;
import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.model.CtpSettings;
import tech.xuanwu.northstar.common.model.ResultBean;
import tech.xuanwu.northstar.restful.GatewayManagementController;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=dev")
public class GatewayManagementWIT {
	
	@MockBean
	private SocketIOServer server;

	@Autowired
	private GatewayManagementController ctrlr;
	
	@After
	public void tearDown() {
		TestMongoUtils.clearDB();
	}

	@Test
	public void shouldCreateGateway() throws Exception{
		ResultBean<Boolean> result = ctrlr.create(TestGatewayFactory.makeMktGateway("TG1", GatewayType.CTP, TestGatewayFactory.makeGatewaySettings(CtpSettings.class), false));
		assertThat(result.getData()).isTrue();

	}

}
