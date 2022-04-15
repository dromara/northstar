package tech.quantit.northstar.data.mongo;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tech.quantit.northstar.common.model.SimAccountDescription;
import tech.quantit.northstar.data.ISimAccountRepository;
import tech.quantit.northstar.data.mongo.po.ContractPO;
import tech.quantit.northstar.data.mongo.po.GatewayDescriptionPO;
import tech.quantit.northstar.data.mongo.po.SimAccountPO;

import java.util.Objects;

/**
 * 模拟账户服务
 * @author : wpxs
 */
public class SimAccountRepoMongoImpl implements ISimAccountRepository{

	private final MongoTemplate mongoTemplate;

	public SimAccountRepoMongoImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	/**
	 * 保存账户信息
	 * @param simAccountDescription
	 */
	@Override
	public void save(SimAccountDescription simAccountDescription) {
		mongoTemplate.save(SimAccountPO.convertFrom(simAccountDescription));
	}

	/**
	 * 查找账户信息
	 * @param accountId
	 * @return
	 */
	@Override
	public SimAccountDescription findById(String accountId) {
		 SimAccountPO gatewayId = mongoTemplate.findOne(Query.query(Criteria.where("gatewayId").is(accountId)), SimAccountPO.class);
		 if (gatewayId == null) {
		 	return null;
		 }
		 return gatewayId.getSimAccount();
	}

	/**
	 * 删除账户信息
	 * @param accountId
	 */
	@Override
	public void deleteById(String accountId) {
		Query query = Query.query(Criteria.where("gatewayId").is(accountId));
		mongoTemplate.remove(query, SimAccountPO.class);
	}

}
