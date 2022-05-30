package tech.quantit.northstar.data.mongo;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.mongo.po.GatewayDescriptionPO;

/**
 * 网关服务
 * @author : wpxs
 */
public class GatewayRepoMongoImpl implements IGatewayRepository{

	private final MongoTemplate mongoTemplate;

	public GatewayRepoMongoImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	/**
	 * 新增网关
	 * @param gatewayDescription : 网关属性
	 */
	@Override
	public void insert(GatewayDescription gatewayDescription) {
		mongoTemplate.insert(GatewayDescriptionPO.convertFrom(gatewayDescription));
	}

	/**
	 * 更新网关
	 * @param gatewayDescription : 网关属性
	 */
	@Override
	public void save(GatewayDescription gatewayDescription) {
		mongoTemplate.save(GatewayDescriptionPO.convertFrom(gatewayDescription));
	}

	/**
	 * 根据网关id删除
	 * @param gatewayId :
	 */
	@Override
	public void deleteById(String gatewayId) {
		Query query = Query.query(Criteria.where("gatewayId").is(gatewayId));
		mongoTemplate.remove(query, GatewayDescriptionPO.class);
	}

	/**
	 * 查询所有网关
	 */
	@Override
	public List<GatewayDescription> findAll() {
		return mongoTemplate.findAll(GatewayDescriptionPO.class).stream().map(GatewayDescriptionPO::getGatewayDescription).collect(Collectors.toList());
	}

	@Override
	public GatewayDescription findById(String gatewayId) {
		GatewayDescriptionPO po = mongoTemplate.findById(gatewayId, GatewayDescriptionPO.class);
		return po.getGatewayDescription();
	}

}
