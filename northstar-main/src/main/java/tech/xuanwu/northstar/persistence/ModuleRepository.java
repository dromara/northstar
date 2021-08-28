package tech.xuanwu.northstar.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import tech.xuanwu.northstar.strategy.common.model.ModuleInfo;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.TradeDescription;

@Repository
public class ModuleRepository {

	@Autowired
	private MongoTemplate mongo;
	
	/*************/
	/**	模组信息	**/
	/*************/
	public boolean saveModuleInfo(ModuleInfo info) {
		mongo.save(info);
		return true;
	}
	
	public List<ModuleInfo> findAllModuleInfo(){
		return mongo.findAll(ModuleInfo.class);
	}
	
	public ModuleInfo findModuleInfo(String moduleId){
		return mongo.findById(moduleId, ModuleInfo.class);
	}
	
	public void deleteModuleInfoById(String moduleId) {
		mongo.remove(Query.query(Criteria.where("moduleName").is(moduleId)), ModuleInfo.class);
	}
	
	/*************/
	/**	模组状态	**/
	/*************/
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
	
	public void removeModuleStatus(String moduleName) {
		mongo.remove(Query.query(Criteria.where("moduleName").is(moduleName)), ModuleStatus.class);
	}
	
	/*************/
	/**	模组成交	**/
	/*************/
	public void saveTradeDescription(TradeDescription tradeDescription) {
		mongo.save(tradeDescription);
	}
	
	public List<TradeDescription> findTradeDescription(String moduleName) {
		return mongo.find(Query.query(Criteria.where("moduleName").is(moduleName)), TradeDescription.class);
	}
	
	public void removeTradeDescription(String moduleName) {
		mongo.remove(Query.query(Criteria.where("moduleName").is(moduleName)), TradeDescription.class);
	}
	
}
