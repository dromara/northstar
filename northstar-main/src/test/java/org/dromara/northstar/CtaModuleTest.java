package org.dromara.northstar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.ComponentAndParamsPair;
import org.dromara.northstar.common.model.ComponentMetaInfo;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleAccountDescription;
import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.ResultBean;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.sim.trade.SimTradeGateway;
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.example.BeginnerExampleStrategy;
import org.dromara.northstar.web.restful.GatewayManagementController;
import org.dromara.northstar.web.restful.LogController;
import org.dromara.northstar.web.restful.ModuleController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOServer;

import io.netty.util.internal.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

/**
 * 模组集成测试
 * @auth KevinHuangwl
 */
@Slf4j
@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=unittest")
class CtaModuleTest {
	
	@Autowired
	GatewayManagementController gatewayCtlr;
	
	@Autowired
	ModuleController moduleCtlr;
	
	@MockBean
	private SocketIOServer socketServer;
	
	@Autowired
	IContractManager contractMgr;
	
	IMarketCenter mktCenter;
	
	@Autowired
	FastEventEngine feEngine;
	
	@Autowired
	LogController logCtlr;
	
	@Autowired
	GatewayManager gatewayMgr;
	
	@Autowired
	ModuleManager moduleMgr;
	
	List<ContractSimpleInfo> contracts = List.of(ContractSimpleInfo.builder()
			.unifiedSymbol("sim9901@SHFE@FUTURES")
			.value("sim9901@SHFE@FUTURES@SIM")
			.channelType(ChannelType.SIM)
			.build());
	
	@BeforeEach
	void prepare() throws Exception {
		when(socketServer.getRoomOperations(anyString())).thenReturn(mock(BroadcastOperations.class));
		when(socketServer.getBroadcastOperations()).thenReturn(mock(BroadcastOperations.class));
		mktCenter = (IMarketCenter) contractMgr;
		
		// 创建一个SIM行情
		gatewayCtlr.create(GatewayDescription.builder()
				.gatewayId("SIM")
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.channelType(ChannelType.SIM)
				.autoConnect(false)
				.subscribedContracts(contracts)
				.settings(new Object())
				.build());
	
		// 创建一个SIM账户
		gatewayCtlr.create(GatewayDescription.builder()
				.gatewayId("模拟账户")
				.gatewayUsage(GatewayUsage.TRADE)
				.channelType(ChannelType.SIM)
				.bindedMktGatewayId("SIM")
				.autoConnect(true)
				.settings(new Object())
				.build());
		
		Thread.sleep(2000);
		
		gatewayCtlr.simMoneyIO("模拟账户", 1000000);
	}
	
	@AfterEach
	void cleanup() {
		moduleCtlr.removeModule("CTA测试");
		moduleCtlr.removeModule("K线测试");
		gatewayCtlr.disconnect("模拟账户");
		gatewayCtlr.remove("模拟账户");
		gatewayCtlr.remove("SIM");
	}
	
	// SIM行情 + 简单策略（模拟盘）
	@Test
	void testCTAModule() throws Exception {
		// 创建一个简单策略
		ComponentMetaInfo cmi = new ComponentMetaInfo();
		cmi.setName("示例-简单策略");
		cmi.setClassName(BeginnerExampleStrategy.class.getName());
		BeginnerExampleStrategy.InitParams params = new BeginnerExampleStrategy.InitParams();
		ComponentAndParamsPair strategySettings = ComponentAndParamsPair.builder()
				.componentMeta(cmi)
				.initParams(params.getMetaInfo().values().stream().toList())
				.build();
		ModuleAccountDescription mad = ModuleAccountDescription.builder()
				.bindedContracts(contracts)
				.accountGatewayId("模拟账户")
				.build();
		moduleCtlr.createModule(ModuleDescription.builder()
				.moduleName("CTA测试")
				.initBalance(100000)
				.closingPolicy(ClosingPolicy.FIRST_IN_FIRST_OUT)
				.defaultVolume(1)
				.moduleCacheDataSize(500)
				.numOfMinPerBar(1)
				.type(ModuleType.SPECULATION)
				.usage(ModuleUsage.UAT)
				.moduleAccountSettingsDescription(List.of(mad))
				.strategySetting(strategySettings)
				.build());
		moduleCtlr.toggleModuleState("CTA测试");
		logCtlr.setModuleLogLevel("CTA测试", LogLevel.TRACE);
		// 开始测试
		log.info("开始测试");
		SimTradeGateway simGateway = (SimTradeGateway) gatewayMgr.get(Identifier.of("模拟账户"));
		IModule module = moduleMgr.get(Identifier.of("CTA测试"));
		Contract c = contractMgr.getContract(ChannelType.SIM, "sim9901").contract();
		long t = System.currentTimeMillis();
		for(int i=0; i<4; i++) {
			log.info("构造模拟TICK");
			Tick tick = Tick.builder()
					.actionDay(CommonUtils.millsToLocalDateTime(t).toLocalDate())
					.actionTime(CommonUtils.millsToLocalDateTime(t).toLocalTime())
					.tradingDay(LocalDate.now())
					.actionTimestamp(t)
					.lastPrice(5000)
					.askPrice(List.of(5001D))
					.bidPrice(List.of(4999D))
					.askVolume(List.of(10000))
					.bidVolume(List.of(10000))
					.channelType(ChannelType.SIM)
					.contract(c)
					.gatewayId("SIM")
					.build();
			mktCenter.onTick(tick);
			module.getModuleContext().onTick(tick);
			simGateway.onTick(tick);
			t += TimeUnit.MINUTES.toMillis(2);
			Thread.sleep(1500);
		}
		log.info("等待成交");
		
		// 期望有正常成交
		ResultBean<List<ModuleDealRecord>> dealRecordResult = moduleCtlr.getDealRecords("CTA测试");
		assertThat(dealRecordResult.getData()).isNotEmpty();
	}
	
	@Test
	void testGetRuntime() throws Exception {
		// 创建一个简单策略
		ComponentMetaInfo cmi = new ComponentMetaInfo();
		cmi.setName("示例-简单策略");
		cmi.setClassName(BeginnerExampleStrategy.class.getName());
		BeginnerExampleStrategy.InitParams params = new BeginnerExampleStrategy.InitParams();
		ComponentAndParamsPair strategySettings = ComponentAndParamsPair.builder()
				.componentMeta(cmi)
				.initParams(params.getMetaInfo().values().stream().toList())
				.build();
		ModuleAccountDescription mad = ModuleAccountDescription.builder()
				.bindedContracts(contracts)
				.accountGatewayId("模拟账户")
				.build();
		moduleCtlr.createModule(ModuleDescription.builder()
				.moduleName("K线测试")
				.initBalance(100000)
				.closingPolicy(ClosingPolicy.FIRST_IN_FIRST_OUT)
				.defaultVolume(1)
				.moduleCacheDataSize(500)
				.numOfMinPerBar(1)
				.type(ModuleType.SPECULATION)
				.usage(ModuleUsage.PLAYBACK)
				.moduleAccountSettingsDescription(List.of(mad))
				.strategySetting(strategySettings)
				.build());
		logCtlr.setModuleLogLevel("K线测试", LogLevel.TRACE);
		// 开始测试
		Contract c = contractMgr.getContract(ChannelType.SIM, "sim9901").contract();
		LocalTime time = LocalTime.now().withSecond(0).withNano(0);
		double price = ThreadLocalRandom.current().nextDouble(5000);
		for(int i=0; i<100; i++) {
			double open = price;
			double delta = ThreadLocalRandom.current().nextDouble(-50, 50);
			double close = open + delta;
			double high = open + ThreadLocalRandom.current().nextDouble(0, 50);
			double low = open - ThreadLocalRandom.current().nextDouble(0, 50);
			Bar bar = Bar.builder()
					.actionDay(LocalDate.now())
					.actionTime(time)
					.tradingDay(LocalDate.now())
					.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(LocalDate.now(), time)))
					.openPrice(open)
					.closePrice(close)
					.highPrice(high)
					.lowPrice(low)
					.channelType(ChannelType.SIM)
					.contract(c)
					.gatewayId("SIM")
					.build();
			feEngine.emitEvent(NorthstarEventType.BAR, bar);
			time = time.plusMinutes(1);
		}
		
		Thread.sleep(1000);
		
		// 期望有K线数据
		ResultBean<ModuleRuntimeDescription> result = moduleCtlr.getModuleRealTimeInfo("K线测试");
		assertThat(result.getData().getDataMap().get("模拟合约9901")).isNotEmpty();
	}
}
