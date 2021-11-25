package tech.xuanwu.northstar.main.persistence;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import tech.xuanwu.northstar.main.playback.PlaybackStatRecord;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleDealRecord;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleInfo;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleTradeRecord;

@Repository
public class ModuleRepository {

	@Autowired
	protected MongoTemplate mongo;
	
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
	public ModuleStatus loadModuleStatus(String moduleName){
		return mongo.findOne(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), ModuleStatus.class);
	}
	
	public void saveModuleStatus(ModuleStatus status) {
		mongo.save(status);
	}
	
	public void removeModuleStatus(String moduleName) {
		mongo.remove(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), ModuleStatus.class);
	}
	
	/*************/
	/**	模组交易	**/
	/*************/
	public void saveDealRecord(ModuleDealRecord dealRecord) {
		mongo.save(dealRecord);
	}
	
	public List<ModuleDealRecord> findDealRecords(String moduleName) {
		return mongo.find(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), ModuleDealRecord.class);
	}
	
	public void removeDealRecords(String moduleName) {
		mongo.remove(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), ModuleDealRecord.class);
	}
	
	/*************/
	/**	模组成交	**/
	/*************/
	public void saveTradeRecord(ModuleTradeRecord tradeRecord) {
		mongo.save(tradeRecord);
	}
	
	public List<ModuleTradeRecord> findTradeRecords(String moduleName){
		return mongo.find(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), ModuleTradeRecord.class);
	}
	
	public void removeTradeRecords(String moduleName) {
		mongo.remove(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), ModuleTradeRecord.class);
	}
	
	/*************/
	/**	模组回测	**/
	/*************/
	public void savePlaybackStatRecord(PlaybackStatRecord playbackRecord) {
		mongo.save(playbackRecord);
	}
	
	public PlaybackStatRecord getPlaybackStatRecord(String moduleName) {
		return mongo.findOne(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), PlaybackStatRecord.class);
	}
	
}
