package tech.quantit.northstar.data.mongo;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundSetOperations;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.data.IContractRepository;
import tech.quantit.northstar.data.mongo.po.ContractPO;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

@Slf4j
public class ContractRepoMongoImpl implements IContractRepository {

	private MongoTemplate mongoTemplate;

	public ContractRepoMongoImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public void batchSave(List<ContractField> contracts) {
		for(ContractField c : contracts) {
			save(c);
		}
	}

	@Override
	public void save(ContractField contract) {
		mongoTemplate.save(ContractPO.convertFrom(contract));
	}

	@Override
	public List<ContractField> findAllByType(ProductClassEnum type) {
		int intDateToday = Integer.parseInt(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		mongoTemplate.findAndRemove(Query.query(Criteria.where("expiredDate").lt(intDateToday)), ContractPO.class);
		return mongoTemplate.find(Query.query(Criteria.where("type").is(type)), ContractPO.class)
				.stream()
				.map(this::convert)
				.filter(Objects::nonNull)
				.toList();
	}

	/**
	 * 根据gatewayId查询合约
	 * TODO 待实现
	 *
	 * @param gatewayId
	 * @return
	 */
	@Override
	public List<ContractField> getByGateWayId(String gatewayId){
		String type = "";
		int intDateToday = Integer.parseInt(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		mongoTemplate.findAndRemove(Query.query(Criteria.where("expiredDate").lt(intDateToday)), ContractPO.class);
		return mongoTemplate.find(Query.query(Criteria.where("gatewayId").is(gatewayId)), ContractPO.class)
				.stream()
				.map(this::convert)
				.filter(Objects::nonNull)
				.toList();
	}

	private ContractField convert(ContractPO po) {
		try {
			return ContractField.parseFrom(po.getData());
		} catch (InvalidProtocolBufferException e) {
			log.warn("", e);
			return null;
		}
	}

}
