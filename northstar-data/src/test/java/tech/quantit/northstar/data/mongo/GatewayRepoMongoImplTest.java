package tech.quantit.northstar.data.mongo;

import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.mongo.po.GatewayDescriptionPO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关服务 测试
 * @author : wpxs
 */
public class GatewayRepoMongoImplTest {

	MongoTemplate mongoTemplate = new MongoTemplate(MongoClients.create("mongodb://localhost:27017"), "TEST_NS_DB");

	IGatewayRepository repo = new GatewayRepoMongoImpl(mongoTemplate);

	GatewayDescription gd1 = GatewayDescription.builder()
			.gatewayId("test")
			.gatewayType(GatewayType.SIM)
			.bindedMktGatewayId("gateway.sim.market.SimMarketGatewayLocal")
			.autoConnect(false)
			.gatewayUsage(GatewayUsage.MARKET_DATA)
			.gatewayAdapterType("gateway.sim.market.SimMarketGatewayLocal")
			.description("test测试")
			.build();

	GatewayDescription gd2 = GatewayDescription.builder()
			.gatewayId("test2")
			.gatewayType(GatewayType.SIM)
			.bindedMktGatewayId("gateway.sim.market.SimMarketGatewayLocal")
			.autoConnect(false)
			.gatewayUsage(GatewayUsage.MARKET_DATA)
			.gatewayAdapterType("gateway.sim.market.SimMarketGatewayLocal")
			.description("test2测试")
			.build();

	@AfterEach
	void clear() {
		mongoTemplate.dropCollection(GatewayDescriptionPO.class);
	}

	@Test
	void testInsert(){
		repo.insert(gd1);
		assertThat(mongoTemplate.findAll(GatewayDescriptionPO.class)).hasSize(1);
	}

	@Test
	void testSave(){
		repo.save(gd1);
		assertThat(mongoTemplate.findAll(GatewayDescriptionPO.class)).hasSize(1);
	}

	@Test
	void testDeleteById(){
		repo.save(gd1);
		repo.deleteById(gd1.getGatewayId());
		assertThat(mongoTemplate.findAll(GatewayDescriptionPO.class)).isEmpty();
	}

	@Test
	void testFindAll(){
		repo.insert(gd1);
		repo.insert(gd2);
		assertThat(repo.findAll()).hasSize(2);
	}
}
