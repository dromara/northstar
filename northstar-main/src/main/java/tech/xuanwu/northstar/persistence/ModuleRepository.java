package tech.xuanwu.northstar.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import tech.xuanwu.northstar.persistence.po.TradePO;
import tech.xuanwu.northstar.strategy.common.model.ModuleInfo;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;

@Repository
public class ModuleRepository {

	@Autowired
	private MongoTemplate mongo;
	
	public void saveModuleInfo(ModuleInfo info) {
		mongo.save(info);
	}
	
	public List<ModuleInfo> findAllModuleInfo(){
		return mongo.findAll(ModuleInfo.class);
	}
	
	public void deleteModuleInfoById(String moduleId) {
		mongo.remove(Query.query(Criteria.where("moduleName").is(moduleId)), ModuleInfo.class);
	}
	
	public Map<String, ModuleStatus> loadModuleStatus(){
		List<ModuleStatus> statusList = mongo.findAll(ModuleStatus.class);
		Map<String, ModuleStatus> statusMap = new HashMap<>(statusList.size());
		for(ModuleStatus ms : statusList) {
			statusMap.put(ms.getModuleName(), ms);
		}
		return statusMap;
	}
	
	public void saveModuleStatus(ModuleStatus status) {
		mongo.save(status);
	}
	
	public void saveTradePO(TradePO po) {
		mongo.save(po);
	}
	
	public TradePO loadLatestTradePO(String moduleName) {
		return mongo.findOne(Query
				.query(Criteria.where("moduleName").is(moduleName))
				.with(Sort.by(Direction.DESC, "tradeTimestamp")), TradePO.class);
	}
}
