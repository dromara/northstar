package tech.quantit.northstar.main.persistence;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.main.persistence.po.ContractPO;

@Slf4j
@Repository
public class ContractRepository implements IContractRepository {

	@Autowired
	private MongoTemplate mongo;
	
	private static final long DAY14MILLISEC = TimeUnit.DAYS.toMillis(14);
	/**
	 * 批量保存合约信息
	 * @param contracts
	 */
	@Override
	public void batchSaveContracts(List<ContractPO> contracts) {
		if(contracts.isEmpty()) {
			return;
		}
		log.debug("保存合约：{}条", contracts.size());
		long start = System.currentTimeMillis();
		for(ContractPO po : contracts) {
			mongo.save(po);
		}
		log.debug("合约保存成功，耗时{}毫秒", System.currentTimeMillis() - start);
	}
	
	/**
	 * 保存合约信息
	 * @param contract
	 */
	@Override
	public void saveContract(ContractPO contract) {
		mongo.save(contract);
	}

	/**
	 * 查询有效合约列表
	 * @return
	 */
	@Override
	public List<ContractPO> getAvailableContracts(){
		log.debug("查询十四天内登记过的有效合约");
		long day14Ago = System.currentTimeMillis() - DAY14MILLISEC;
		return mongo.find(Query.query(Criteria.where("updateTime").gt(day14Ago)), ContractPO.class);
	}
}
