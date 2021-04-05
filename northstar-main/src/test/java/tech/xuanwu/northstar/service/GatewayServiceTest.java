package tech.xuanwu.northstar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.GatewayUsage;
import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.engine.event.EventEngine;
import tech.xuanwu.northstar.model.CtpSettings;
import tech.xuanwu.northstar.persistence.GatewayRepository;
import tech.xuanwu.northstar.persistence.po.GatewayPO;

public class GatewayServiceTest {
	
	GatewayService service = new GatewayService();
	
	@Before
	public void prepare() {
		service.eventBus = mock(InternalEventBus.class);
		service.eventEngine = mock(EventEngine.class);
		service.gatewayRepo = mock(GatewayRepository.class);
		
	}

	@Test
	public void testCreateGateway() {
		CtpSettings settings = new CtpSettings();
		settings.setAppId("app123456");
		settings.setAuthCode("auth321564");
		settings.setBrokerId("pingan");
		settings.setMdHost("127.0.0.1");
		settings.setMdPort("8080");
		settings.setPassword("adslfkjals");
		settings.setTdHost("127.0.0.1");
		settings.setTdPort("8081");
		settings.setUserId("kevin");
		settings.setUserProductInfo("productioninfo");
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId("testGateway")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.settings(settings)
				.build();
		service.createGateway(gd);
		
		assertThat(service.marketGatewayMap.containsKey("testGateway")).isTrue();
		assertThat(service.gatewayMap.size()).isEqualTo(1);
		
		verify(service.gatewayRepo).insert(ArgumentMatchers.any(GatewayPO.class));
	}
	
	@Test
	public void testCreateGateway2() {
		CtpSettings settings = new CtpSettings();
		settings.setAppId("app123456");
		settings.setAuthCode("auth321564");
		settings.setBrokerId("pingan");
		settings.setMdHost("127.0.0.1");
		settings.setMdPort("8080");
		settings.setPassword("adslfkjals");
		settings.setTdHost("127.0.0.1");
		settings.setTdPort("8081");
		settings.setUserId("kevin");
		settings.setUserProductInfo("productioninfo");
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId("testGateway")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.TRADE)
				.settings(settings)
				.autoConnect(true)
				.build();
		service.createGateway(gd);
		
		assertThat(service.traderGatewayMap.containsKey("testGateway")).isTrue();
		assertThat(service.gatewayMap.size()).isEqualTo(1);
		
		verify(service.gatewayRepo).insert(ArgumentMatchers.any(GatewayPO.class));
	}

	@Test
	public void testUpdateGateway() {
		testCreateGateway();
		
		CtpSettings settings = new CtpSettings();
		settings.setAppId("app123456");
		settings.setAuthCode("auth321564");
		settings.setBrokerId("pingan");
		settings.setMdHost("127.0.0.1");
		settings.setMdPort("8080");
		settings.setPassword("adslfkjals");
		settings.setTdHost("127.0.0.1");
		settings.setTdPort("8081");
		settings.setUserId("kevin");
		settings.setUserProductInfo("productioninfo");
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId("testGateway")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.settings(settings)
				.build();
		service.updateGateway(gd);
		
		assertThat(service.traderGatewayMap.size()).isEqualTo(0);
		assertThat(service.marketGatewayMap.size()).isEqualTo(1);
		
		verify(service.gatewayRepo).save(ArgumentMatchers.any(GatewayPO.class));
	}

	@Test
	public void testDeleteGateway() {
		testCreateGateway();
		
		service.deleteGateway("testGateway");
		assertThat(service.traderGatewayMap.size()).isEqualTo(0);
		assertThat(service.marketGatewayMap.size()).isEqualTo(0);
		assertThat(service.gatewayMap.size()).isEqualTo(0);
		
		verify(service.gatewayRepo).deleteById("testGateway");
	}

	@Test
	public void testFindAllGateway() {
		CtpSettings settings = new CtpSettings();
		settings.setAppId("app123456");
		settings.setAuthCode("auth321564");
		settings.setBrokerId("pingan");
		settings.setMdHost("127.0.0.1");
		settings.setMdPort("8080");
		settings.setPassword("adslfkjals");
		settings.setTdHost("127.0.0.1");
		settings.setTdPort("8081");
		settings.setUserId("kevin");
		settings.setUserProductInfo("productioninfo");
		GatewayDescription gd1 = GatewayDescription.builder()
				.gatewayId("testGateway1")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.settings(settings)
				.build();
		GatewayDescription gd2 = GatewayDescription.builder()
				.gatewayId("testGateway2")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.TRADE)
				.settings(settings)
				.build();
		
		service.createGateway(gd1);
		service.createGateway(gd2);
		
		assertThat(service.findAllGateway().size()).isEqualTo(2);
	}

	@Test
	public void testFindAllMarketGateway() {
		CtpSettings settings = new CtpSettings();
		settings.setAppId("app123456");
		settings.setAuthCode("auth321564");
		settings.setBrokerId("pingan");
		settings.setMdHost("127.0.0.1");
		settings.setMdPort("8080");
		settings.setPassword("adslfkjals");
		settings.setTdHost("127.0.0.1");
		settings.setTdPort("8081");
		settings.setUserId("kevin");
		settings.setUserProductInfo("productioninfo");
		GatewayDescription gd1 = GatewayDescription.builder()
				.gatewayId("testGateway1")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.settings(settings)
				.build();
		GatewayDescription gd2 = GatewayDescription.builder()
				.gatewayId("testGateway2")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.TRADE)
				.settings(settings)
				.build();
		
		service.createGateway(gd1);
		service.createGateway(gd2);
		
		assertThat(service.findAllMarketGateway().get(0).getGatewayUsage()).isEqualTo(GatewayUsage.MARKET_DATA);
	}

	@Test
	public void testFindAllTraderGateway() {
		CtpSettings settings = new CtpSettings();
		settings.setAppId("app123456");
		settings.setAuthCode("auth321564");
		settings.setBrokerId("pingan");
		settings.setMdHost("127.0.0.1");
		settings.setMdPort("8080");
		settings.setPassword("adslfkjals");
		settings.setTdHost("127.0.0.1");
		settings.setTdPort("8081");
		settings.setUserId("kevin");
		settings.setUserProductInfo("productioninfo");
		GatewayDescription gd1 = GatewayDescription.builder()
				.gatewayId("testGateway1")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.settings(settings)
				.build();
		GatewayDescription gd2 = GatewayDescription.builder()
				.gatewayId("testGateway2")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.TRADE)
				.settings(settings)
				.build();
		
		service.createGateway(gd1);
		service.createGateway(gd2);
		
		assertThat(service.findAllTraderGateway().get(0).getGatewayUsage()).isEqualTo(GatewayUsage.TRADE);
	}

	@Test
	public void testConnect() {
		testCreateGateway();
		
		service.connect("testGateway");
		verify(service.eventBus).post(ArgumentMatchers.any(NorthstarEvent.class));
	}

	@Test
	public void testDisconnect() {
		testCreateGateway2();
		
		service.disconnect("testGateway");
		verify(service.eventBus, times(2)).post(ArgumentMatchers.any(NorthstarEvent.class));
	}

}
