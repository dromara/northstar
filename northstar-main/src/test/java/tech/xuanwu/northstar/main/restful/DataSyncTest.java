package tech.xuanwu.northstar.main.restful;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.ReturnCode;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.model.ResultBean;
import tech.xuanwu.northstar.common.model.SimSettings;
import tech.xuanwu.northstar.common.model.SimpleContractInfo;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.main.NorthstarApplication;
import tech.xuanwu.northstar.main.restful.DataSyncController;
import tech.xuanwu.northstar.main.restful.GatewayManagementController;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
public class DataSyncTest {
	
	@MockBean
	private SocketIOServer server;
	
	@Autowired
	private DataSyncController ctrlr;
	
	@Autowired
	private GatewayManagementController gatewayCtrlr;
	
	@MockBean
	private SocketIOMessageEngine msgEngine;
	
	
	
	@Before
	public void setUp() throws Exception {
		gatewayCtrlr.create(TestGatewayFactory.makeMktGateway("TG1", GatewayType.SIM, TestGatewayFactory.makeGatewaySettings(SimSettings.class), true));
		gatewayCtrlr.create(TestGatewayFactory.makeTrdGateway("TG2", "TG1", GatewayType.SIM, TestGatewayFactory.makeGatewaySettings(SimSettings.class), true));
		Thread.sleep(100);
		
	}

	@After
	public void tearDown() throws Exception {
		gatewayCtrlr.disconnect("TG2");
		gatewayCtrlr.disconnect("TG1");
		Thread.sleep(100);
		gatewayCtrlr.remove("TG2");
		gatewayCtrlr.remove("TG1");
		TestMongoUtils.clearDB();
	}

	@Test
	public void testDataSync() throws Exception {
		ctrlr.sync();
		Thread.sleep(100);
		verify(msgEngine).emitEvent(argThat(e -> e.getEvent() == NorthstarEventType.ACCOUNT), eq(AccountField.class));
	}
	
	@Test
	public void testGetHistoryBar() throws Exception {
		ctrlr.historyBars("TG1", "rb2110@SHFE@FUTURES", null, null);
		Thread.sleep(100);
		verify(msgEngine).emitEvent(argThat(e -> e.getEvent() == NorthstarEventType.HIS_BAR), eq(BarField.class));
	}
	
	@Test
	public void shouldGetAvailableContracts() throws Exception {
		Thread.sleep(1000);
		ResultBean<List<SimpleContractInfo>> result = ctrlr.availableContracts();
		assertThat(result.getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}

}
