package tech.quantit.northstar.main.persistence;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.main.persistence.po.ModulePositionPO;
import tech.quantit.northstar.main.playback.PlaybackStatRecord;
import tech.quantit.northstar.strategy.api.model.ModuleDealRecord;
import tech.quantit.northstar.strategy.api.model.ModuleInfo;
import tech.quantit.northstar.strategy.api.model.ModuleTradeRecord;

@Slf4j
@Repository
public class ModuleRepository {

	@Autowired
	protected MongoTemplate mongo;

	private static final String MODULE_NAME = "moduleName";

	/*************/
	/** 模组信息 **/
	/*************/
	public boolean saveModuleInfo(ModuleInfo info) {
		if(log.isDebugEnabled()) {			
			log.debug("[{}] 保存模组信息", info.getModuleName());
		}
		mongo.save(info);
		return true;
	}

	public List<ModuleInfo> findAllModuleInfo() {
		return mongo.findAll(ModuleInfo.class);
	}

	public ModuleInfo findModuleInfo(String moduleId) {
		return mongo.findById(moduleId, ModuleInfo.class);
	}

	public void deleteModuleInfoById(String moduleName) {
		if(log.isDebugEnabled()) {			
			log.debug("[{}] 删除模组信息", moduleName);
		}
		mongo.remove(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), ModuleInfo.class);
	}

	/*************/
	/**  模组持仓  **/
	/*************/
	public ModulePositionPO loadModulePosition(String moduleName) {
		return mongo.findOne(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), ModulePositionPO.class);
	}

	public void saveModulePosition(ModulePositionPO status) {
		if(log.isDebugEnabled()) {			
			log.debug("[{}] 保存持仓记录", status.getModuleName());
		}
		mongo.save(status);
	}

	public void removeModulePosition(String moduleName) {
		if(log.isDebugEnabled()) {			
			log.debug("[{}] 清除持仓记录", moduleName);
		}
		mongo.remove(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), ModulePositionPO.class);
	}

	/*************/
	/** 模组交易 **/
	/*************/
	public void saveDealRecord(ModuleDealRecord dealRecord) {
		if(log.isDebugEnabled()) {			
			log.debug("[{}] 保存交易记录", dealRecord.getModuleName());
		}
		mongo.save(dealRecord);
	}

	public List<ModuleDealRecord> findDealRecords(String moduleName) {
		return mongo.find(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), ModuleDealRecord.class);
	}

	public void removeDealRecords(String moduleName) {
		if(log.isDebugEnabled()) {			
			log.debug("[{}] 清除交易记录", moduleName);
		}
		mongo.remove(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), ModuleDealRecord.class);
	}

	/*************/
	/** 模组成交 **/
	/*************/
	public void saveTradeRecord(ModuleTradeRecord tradeRecord) {
		if(log.isDebugEnabled()) {			
			log.debug("[{}] 保存模组成交", tradeRecord.getModuleName());
		}
		mongo.save(tradeRecord);
	}

	public List<ModuleTradeRecord> findTradeRecords(String moduleName) {
		return mongo.find(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), ModuleTradeRecord.class);
	}

	public void removeTradeRecords(String moduleName) {
		if(log.isDebugEnabled()) {			
			log.debug("[{}] 清除成交记录", moduleName);
		}
		mongo.remove(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), ModuleTradeRecord.class);
	}

	/*************/
	/** 模组回测 **/
	/*************/
	public void savePlaybackStatRecord(PlaybackStatRecord playbackRecord) {
		if(log.isDebugEnabled()) {			
			log.debug("[{}] 保存模组回测信息", playbackRecord.getModuleName());
		}
		mongo.save(playbackRecord);
	}

	public PlaybackStatRecord getPlaybackStatRecord(String moduleName) {
		return mongo.findOne(Query.query(Criteria.where(MODULE_NAME).is(moduleName)), PlaybackStatRecord.class);
	}

}
