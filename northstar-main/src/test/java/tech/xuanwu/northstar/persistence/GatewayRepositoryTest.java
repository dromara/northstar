package tech.xuanwu.northstar.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import tech.xuanwu.northstar.persistence.po.GatewayPO;
import tech.xuanwu.northstar.persistence.po.GatewayPO.GatewayType;
import tech.xuanwu.northstar.persistence.po.GatewayPO.GatewayUsage;

@RunWith(SpringRunner.class)
@DataMongoTest
@TestPropertySource("classpath:application-unittest.properties")
public class GatewayRepositoryTest {

	@Autowired
	GatewayRepository repo;
	
	@Before
	public void pretest() {
		repo.deleteAll();
	}
	
	@After
	public void clear() {
		repo.deleteAll();
	}
	
	@Test(expected = Exception.class)
	public void testInsertS() {
		GatewayPO po = GatewayPO.builder()
				.gatewayId("TestGateway")
				.gatewayAdapterType("tech.xuanwu.northstar.gateway.ctp.x64v6v3v15v.CtpGatewayAdapter")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.build();
		
		repo.insert(po);
		repo.insert(po);
	}

	@Test
	public void testSave() {
		GatewayPO po = GatewayPO.builder()
				.gatewayId("TestGateway2")
				.gatewayAdapterType("tech.xuanwu.northstar.gateway.ctp.x64v6v3v15v.CtpGatewayAdapter")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.build();
		
		repo.save(po);
		repo.save(po);
		
		assertThat(repo.count()).isEqualTo(1);
	}

	@Test
	public void testDelete() {
		GatewayPO po = GatewayPO.builder()
				.gatewayId("TestGateway3")
				.gatewayAdapterType("tech.xuanwu.northstar.gateway.ctp.x64v6v3v15v.CtpGatewayAdapter")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.build();
		
		repo.save(po);
		assertThat(repo.count()).isEqualTo(1);
		repo.delete(po);
		assertThat(repo.count()).isEqualTo(0);
	}
	
	@Test
	public void testFindAll() {
		GatewayPO po1 = GatewayPO.builder()
				.gatewayId("TestGateway4")
				.gatewayAdapterType("tech.xuanwu.northstar.gateway.ctp.x64v6v3v15v.CtpGatewayAdapter")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.build();
		
		GatewayPO po2 = GatewayPO.builder()
				.gatewayId("TestGateway5")
				.gatewayAdapterType("tech.xuanwu.northstar.gateway.ctp.x64v6v3v15v.CtpGatewayAdapter")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.build();
		
		repo.save(po1);
		repo.save(po2);
		assertThat(repo.count()).isEqualTo(2);
	}

}
