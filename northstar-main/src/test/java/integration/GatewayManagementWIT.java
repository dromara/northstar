package integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.After;
import org.junit.Before;
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
import tech.xuanwu.northstar.common.constant.GatewayUsage;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.model.CtpSettings;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.common.model.ResultBean;
import tech.xuanwu.northstar.common.model.SimSettings;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.restful.GatewayManagementController;
import xyz.redtorch.pb.CoreField.AccountField;

/**
 * GatewayManagement接口白盒测试类
 * @author kevin
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
public class GatewayManagementWIT {
	
	@MockBean
	private SocketIOServer server;

	@Autowired
	private GatewayManagementController ctrlr;
	
	@MockBean
	private FastEventEngine feEngine;
	
	@Before
	public void SetupContext() {
		TestMongoUtils.clearDB();
	}
	
	@After
	public void tearDown() {
		TestMongoUtils.clearDB();
	}

	@Test
	public void shouldCreateGateway() throws Exception{
		ResultBean<Boolean> result = ctrlr.create(TestGatewayFactory.makeMktGateway("TG1", GatewayType.CTP, TestGatewayFactory.makeGatewaySettings(CtpSettings.class), false));
		assertThat(result.getData()).isTrue();

		ResultBean<Boolean> result2 = ctrlr.create(TestGatewayFactory.makeTrdGateway("TG2", "TG1", GatewayType.SIM, TestGatewayFactory.makeGatewaySettings(SimSettings.class), false));
		assertThat(result2.getData()).isTrue();
	}

	@Test
	public void shouldFindCreatedGateway() throws Exception {
		shouldCreateGateway();
		
		ResultBean<List<GatewayDescription>> result = ctrlr.list(GatewayUsage.MARKET_DATA.toString());
		assertThat(result.getData().size()).isEqualTo(1);
		assertThat(result.getData().get(0).getGatewayId()).isEqualTo("TG1");
		
		ResultBean<List<GatewayDescription>> result2 = ctrlr.list(GatewayUsage.TRADE.toString());
		assertThat(result2.getData().size()).isEqualTo(1);
		assertThat(result2.getData().get(0).getGatewayId()).isEqualTo("TG2");
	}

	@Test
	public void shouldUpdateGateway() throws Exception {
		shouldCreateGateway();
		
		ResultBean<Boolean> result = ctrlr.modify(TestGatewayFactory.makeTrdGateway("TG2", "TG1", GatewayType.CTP, TestGatewayFactory.makeGatewaySettings(CtpSettings.class), false));
		assertThat(result.getData()).isTrue();
	}
	
	
	@Test
	public void shouldRemoveGateway() throws Exception {
		shouldCreateGateway();
		
		ResultBean<Boolean> result = ctrlr.remove("TG2");
		assertThat(result.getData()).isTrue();
	}
	
	@Test
	public void shouldSuccessWhenGettingState() throws Exception {
		ctrlr.create(TestGatewayFactory.makeMktGateway("TG1", GatewayType.SIM, TestGatewayFactory.makeGatewaySettings(SimSettings.class), true));
		assertThat(ctrlr.getGatewayActive("TG1").getData()).isFalse();
		ctrlr.connect("TG1");
		Thread.sleep(1000);
		assertThat(ctrlr.getGatewayActive("TG1").getData()).isTrue();
		
	}
	
	@Test
	public void shouldSuccessWhenConnecting() throws Exception {
		ctrlr.create(TestGatewayFactory.makeMktGateway("TG1", GatewayType.SIM, TestGatewayFactory.makeGatewaySettings(SimSettings.class), false));
		ResultBean<Boolean> result = ctrlr.connect("TG1");
		assertThat(result.getData()).isTrue();
	}
	
	@Test
	public void shouldSuccessWhenDisconnecting() throws Exception {
		ctrlr.create(TestGatewayFactory.makeMktGateway("TG1", GatewayType.SIM, TestGatewayFactory.makeGatewaySettings(SimSettings.class), true));
		ResultBean<Boolean> result = ctrlr.disconnect("TG1");
		assertThat(result.getData()).isTrue();
	}
	
	@Test
	public void shouldIncreaseBalance() throws Exception {
		shouldCreateGateway();
		ResultBean<Boolean> result = ctrlr.simMoneyIO("TG2", 20000);
		assertThat(result.getData()).isTrue();
		verify(feEngine, times(2)).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getBalance() == 20000));
	}
	
	@Test
	public void shouldDecreaseBalance() throws Exception {
		shouldIncreaseBalance();
		ResultBean<Boolean> result = ctrlr.simMoneyIO("TG2", -1000);
		assertThat(result.getData()).isTrue();
		verify(feEngine).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getBalance() == 19000));
	}
	
}
