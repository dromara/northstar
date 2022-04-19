package tech.quantit.northstar.data.mongo;

import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tech.quantit.northstar.common.model.SimAccountDescription;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.ISimAccountRepository;
import tech.quantit.northstar.data.mongo.po.GatewayDescriptionPO;
import tech.quantit.northstar.data.mongo.po.SimAccountPO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模拟账户服务测试
 * @author : wpxs
 */
public class SimAccountRepoMongoImplTest {

	MongoTemplate mongoTemplate = new MongoTemplate(MongoClients.create("mongodb://localhost:27017"), "TEST_NS_DB");

	ISimAccountRepository repo = new SimAccountRepoMongoImpl(mongoTemplate);

	SimAccountDescription sd1 = SimAccountDescription.builder()
			.gatewayId("test")
			.totalCommission(1.0)
			.totalCloseProfit(1.0)
			.totalDeposit(1.0)
			.totalWithdraw(1.0)
			.transactionFee(1)
			.build();

	@AfterEach
	void clear() {
		mongoTemplate.dropCollection(SimAccountPO.class);
	}

	@Test
	void testSave(){
		repo.save(sd1);
		assertThat(mongoTemplate.findAll(SimAccountPO.class)).hasSize(1);
	}

	@Test
	void testFindById(){
		repo.save(sd1);
		SimAccountDescription byId = repo.findById(sd1.getGatewayId());
		assertThat(byId).isNotNull();
	}

	@Test
	void testDeleteById(){
		repo.save(sd1);
		repo.deleteById(sd1.getGatewayId());
		assertThat(mongoTemplate.findAll(SimAccountPO.class)).isEmpty();
	}
}
