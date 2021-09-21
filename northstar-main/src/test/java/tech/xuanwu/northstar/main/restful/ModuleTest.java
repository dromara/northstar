package tech.xuanwu.northstar.main.restful;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.corundumstudio.socketio.SocketIOServer;

import common.TestMongoUtils;
import tech.xuanwu.northstar.common.constant.ReturnCode;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.gateway.api.TradeGateway;
import tech.xuanwu.northstar.main.NorthstarApplication;
import tech.xuanwu.northstar.main.manager.GatewayAndConnectionManager;
import tech.xuanwu.northstar.strategy.common.constants.ModuleType;
import tech.xuanwu.northstar.strategy.common.model.ModulePosition;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleInfo;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentAndParamsPair;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentField;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentMetaInfo;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
public class ModuleTest {
	
	@Autowired
	private ModuleController ctrlr;

	@MockBean
	private GatewayAndConnectionManager gatewayConnMgr;
	
	@MockBean
	private SocketIOServer server;
	
	@MockBean
	private ContractManager contractMgr;
	
	@Before
	public void setUp() throws Exception {
		when(contractMgr.getContract("rb2210@SHFE@FUTURES")).thenReturn(ContractField.newBuilder().setUnifiedSymbol("rb2210@SHFE@FUTURES").build());
	}

	@After
	public void tearDown() throws Exception {
		TestMongoUtils.clearDB();
	}
	
	// 获取模组交易策略元配置
	@Test
	public void shouldHaveSampleDealer() {
		List<ComponentMetaInfo> dealers = ctrlr.getRegisteredDealers().getData();
		assertThat(dealers.stream().filter(c -> c.getName().equals("示例交易策略")).findAny().isPresent()).isTrue();
	}
	// 获取模组信号策略元配置
	@Test
	public void shouldHaveSamplePolicy() {
		List<ComponentMetaInfo> signalPolicies = ctrlr.getRegisteredSignalPolicies().getData();
		assertThat(signalPolicies.stream().filter(c -> c.getName().equals("示例策略")).findAny().isPresent()).isTrue();
	}
	// 获取模组风控策略元配置
	@Test
	public void shouldHaveBasicRiskRules() {
		List<ComponentMetaInfo> rules = ctrlr.getRegisteredRiskControlRules().getData();
		assertThat(rules.stream().filter(c -> c.getName().equals("模组占用账户资金限制")).findAny().isPresent()).isTrue();
		assertThat(rules.stream().filter(c -> c.getName().equals("日内开仓次数限制")).findAny().isPresent()).isTrue();
		assertThat(rules.stream().filter(c -> c.getName().equals("委托超价限制")).findAny().isPresent()).isTrue();
		assertThat(rules.stream().filter(c -> c.getName().equals("委托超时限制")).findAny().isPresent()).isTrue();
	}

	// 新增模组
	@Test
	public void shouldSuccessfullyCreate() throws Exception {
		GatewayConnection conn = mock(GatewayConnection.class);
		GatewayDescription gwDes = mock(GatewayDescription.class);
		when(gwDes.getBindedMktGatewayId()).thenReturn("testGw");
		when(conn.getGwDescription()).thenReturn(gwDes);
		when(gatewayConnMgr.getGatewayConnectionById("testGateway")).thenReturn(conn);
		TradeGateway gateway = mock(TradeGateway.class);
		when(gateway.getGatewaySetting()).thenReturn(GatewaySettingField.newBuilder().build());
		when(gatewayConnMgr.getGatewayById("testGateway")).thenReturn(gateway);
		ComponentMetaInfo dealer = ctrlr.getRegisteredDealers().getData().stream().filter(c -> c.getName().equals("示例交易策略")).findAny().get();
		ComponentMetaInfo signalPolicy = ctrlr.getRegisteredSignalPolicies().getData().stream().filter(c -> c.getName().equals("示例策略")).findAny().get();
		Map<String, ComponentField> paramsMap = ctrlr.getComponentParams(dealer).getData();
		paramsMap.get("bindedUnifiedSymbol").setValue("rb2210@SHFE@FUTURES");
		ComponentAndParamsPair signalPolicyMeta = ComponentAndParamsPair.builder()
				.componentMeta(signalPolicy)
				.initParams(ctrlr.getComponentParams(signalPolicy).getData().values().stream().collect(Collectors.toList()))
				.build();
		ComponentAndParamsPair dealerMeta = ComponentAndParamsPair.builder()
				.componentMeta(dealer)
				.initParams(paramsMap.values().stream().collect(Collectors.toList()))
				.build();
		ModuleInfo info = ModuleInfo.builder()
				.moduleName("testModule")
				.enabled(true)
				.type(ModuleType.CTA)
				.accountGatewayId("testGateway")
				.signalPolicy(signalPolicyMeta)
				.dealer(dealerMeta)
				.riskControlRules(Collections.EMPTY_LIST)
				.build();
		
		assertThat(ctrlr.createModule(info).getData()).isTrue();
	}
	
	
	// 修改模组
	@Test
	public void shouldSuccessfullyModify() throws Exception {
		GatewayConnection conn = mock(GatewayConnection.class);
		GatewayDescription gwDes = mock(GatewayDescription.class);
		when(gwDes.getBindedMktGatewayId()).thenReturn("testGw");
		when(conn.getGwDescription()).thenReturn(gwDes);
		when(gatewayConnMgr.getGatewayConnectionById("testGateway")).thenReturn(conn);
		when(gatewayConnMgr.getGatewayById("testGateway")).thenReturn(mock(TradeGateway.class));
		ComponentMetaInfo dealer = ctrlr.getRegisteredDealers().getData().stream().filter(c -> c.getName().equals("示例交易策略")).findAny().get();
		ComponentMetaInfo signalPolicy = ctrlr.getRegisteredSignalPolicies().getData().stream().filter(c -> c.getName().equals("示例策略")).findAny().get();
		ComponentAndParamsPair signalPolicyMeta = ComponentAndParamsPair.builder()
				.componentMeta(signalPolicy)
				.initParams(ctrlr.getComponentParams(signalPolicy).getData().values().stream().collect(Collectors.toList()))
				.build();
		ComponentAndParamsPair dealerMeta = ComponentAndParamsPair.builder()
				.componentMeta(dealer)
				.initParams(ctrlr.getComponentParams(dealer).getData().values().stream().collect(Collectors.toList()))
				.build();
		ModuleInfo info = ModuleInfo.builder()
				.moduleName("testModule")
				.enabled(false)
				.type(ModuleType.CTA)
				.accountGatewayId("testGateway")
				.signalPolicy(signalPolicyMeta)
				.dealer(dealerMeta)
				.riskControlRules(Collections.EMPTY_LIST)
				.build();
		
		ctrlr.createModule(info);
		info.setEnabled(true);
		assertThat(ctrlr.updateModule(info).getData()).isTrue();
	}
	
	// 查询模组
	@Test
	public void shouldFindModule() {
		assertThat(ctrlr.getAllModules().getData()).isNotNull();
	}
	
	// 查询模组引用数据
	@Test
	public void shouldGetModuleDataRef() throws Exception {
		shouldSuccessfullyCreate();
		assertThat(ctrlr.getModuleDataRef("testModule").getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	
	@Test
	public void shouldGetModuleInfo() throws Exception {
		shouldSuccessfullyCreate();
		assertThat(ctrlr.getModuleRealTimeInfo("testModule").getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldCreateModulePosition() throws Exception {
		shouldSuccessfullyCreate();
		ModulePosition position = ModulePosition.builder()
				.unifiedSymbol("rb2210@SHFE@FUTURES")
				.multiplier(10)
				.openTime(System.currentTimeMillis())
				.openPrice(1234)
				.openTradingDay("20210609")
				.positionDir(PositionDirectionEnum.PD_Long)
				.build();
		assertThat(ctrlr.createPosition("testModule", position).getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldUpdateModulePosition() throws Exception {
		shouldCreateModulePosition();
		ModulePosition position = ModulePosition.builder()
				.unifiedSymbol("rb2210@SHFE@FUTURES")
				.multiplier(10)
				.openTime(System.currentTimeMillis())
				.openPrice(2000)
				.openTradingDay("20210609")
				.positionDir(PositionDirectionEnum.PD_Long)
				.build();
		assertThat(ctrlr.updatePosition("testModule", position).getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldRemoveModulePosition() throws Exception {
		shouldCreateModulePosition();
		assertThat(ctrlr.removePosition("testModule", "rb2210@SHFE@FUTURES", PositionDirectionEnum.PD_Long).getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
}
