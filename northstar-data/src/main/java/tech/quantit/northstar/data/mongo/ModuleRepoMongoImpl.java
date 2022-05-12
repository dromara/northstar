package tech.quantit.northstar.data.mongo;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import tech.quantit.northstar.common.model.ModuleDealRecord;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.data.mongo.po.ModuleDealRecordPO;
import tech.quantit.northstar.data.mongo.po.ModuleRuntimeDescriptionPO;
import tech.quantit.northstar.data.mongo.po.ModuleDescriptionPO;

/**
 * 模块服务
 * @author : wpxs
 */
public class ModuleRepoMongoImpl implements IModuleRepository{

	private final MongoTemplate mongoTemplate;

	public ModuleRepoMongoImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	/**
	 * 保存模组配置信息
	 * @param moduleSettingsDescription
	 */
	@Override
	public void saveSettings(ModuleDescription moduleSettingsDescription) {
		mongoTemplate.save(ModuleDescriptionPO.convertFrom(moduleSettingsDescription));
	}

	/**
	 * 查询模组配置信息
	 * @param moduleName
	 * @return
	 */
	@Override
	public ModuleDescription findSettingsByName(String moduleName) {
		ModuleDescriptionPO msd = mongoTemplate.findOne(Query.query(Criteria.where("moduleName").is(moduleName)), ModuleDescriptionPO.class);
		if (msd == null) {
			return null;
		}
		return msd.getModuleSettingsDescription();
	}

	/**
	 * 查询所有模组配置信息
	 * @return
	 */
	@Override
	public List<ModuleDescription> findAllSettings() {
		return mongoTemplate.findAll(ModuleDescriptionPO.class).stream().map(ModuleDescriptionPO::getModuleSettingsDescription).collect(Collectors.toList());
	}

	/**
	 * 移除模组配置信息
	 * @param moduleName
	 */
	@Override
	public void deleteSettingsByName(String moduleName) {
		Query query = Query.query(Criteria.where("moduleName").is(moduleName));
		mongoTemplate.remove(query, ModuleDescriptionPO.class);
	}

	/**
	 * 保存模组运行状态信息
	 * @param moduleDescription
	 */
	@Override
	public void saveRuntime(ModuleRuntimeDescription moduleDescription) {
		mongoTemplate.save(ModuleRuntimeDescriptionPO.convertFrom(moduleDescription));
	}

	/**
	 * 获取模组运行状态信息
	 * @param moduleName
	 * @return
	 */
	@Override
	public ModuleRuntimeDescription findRuntimeByName(String moduleName) {
		ModuleRuntimeDescriptionPO md = mongoTemplate.findOne(Query.query(Criteria.where("moduleName").is(moduleName)), ModuleRuntimeDescriptionPO.class);
		if (md == null) {
			return null;
		}
		return md.getModuleDescription();
	}

	/**
	 * 移除模组运行状态信息
	 * @param moduleName
	 */
	@Override
	public void deleteRuntimeByName(String moduleName) {
		Query query = Query.query(Criteria.where("moduleName").is(moduleName));
		mongoTemplate.remove(query, ModuleRuntimeDescriptionPO.class);
	}

	/**
	 * 保存模组交易记录
	 * @param dealRecord
	 */
	@Override
	public void saveDealRecord(ModuleDealRecord dealRecord) {
		mongoTemplate.save(ModuleDealRecordPO.convertFrom(dealRecord));
	}

	/**
	 * 查询模组全部交易记录
	 * @param moduleName
	 * @return
	 */
	@Override
	public List<ModuleDealRecord> findAllDealRecords(String moduleName) {
		return mongoTemplate.find(Query.query(Criteria.where("moduleName").is(moduleName)),ModuleDealRecordPO.class).stream().map(ModuleDealRecordPO::getModuleDealRecord).collect(Collectors.toList());
	}

	/**
	 * 删除模组全部交易记录
	 * @param moduleName
	 */
	@Override
	public void removeAllDealRecords(String moduleName) {
		Query query = Query.query(Criteria.where("moduleName").is(moduleName));
		mongoTemplate.remove(query,ModuleDealRecordPO.class);
	}

}
