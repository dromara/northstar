package tech.xuanwu.northstar.persistence;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import tech.xuanwu.northstar.strategy.common.model.ModuleInfo;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleStatusEntity;
import tech.xuanwu.northstar.strategy.common.model.entity.TradeDescriptionEntity;

@Repository
public class ModuleRepository {

	@Autowired
	private MongoTemplate mongo;
	
	private static final String MODULE_NAME = "moduleName";
	
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
		mongo.remove(Query.query(Criteria.where(MODULE_NAME).is(moduleId)), ModuleInfo.class);
	}
	
	/*************/
	/**	模组状态	**/
	/*************/
	public ModuleStatusEntity loadModuleStatus(String moduleName){
		return mongo.findOne(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), ModuleStatusEntity.class);
	}
	
	public void saveModuleStatus(ModuleStatusEntity status) {
		mongo.save(status);
	}
	
	public void removeModuleStatus(String moduleName) {
		mongo.remove(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), ModuleStatusEntity.class);
	}
	
	/*************/
	/**	模组成交	**/
	/*************/
	public void saveTradeDescription(TradeDescriptionEntity tradeDescription) {
		mongo.save(tradeDescription);
	}
	
	public List<TradeDescriptionEntity> findTradeDescription(String moduleName) {
		return mongo.find(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), TradeDescriptionEntity.class);
	}
	
	public void removeTradeDescription(String moduleName) {
		mongo.remove(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), TradeDescriptionEntity.class);
	}
	
}
