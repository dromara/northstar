package tech.quantit.northstar.data;

import java.util.List;

import tech.quantit.northstar.common.model.ModuleDealRecord;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.common.model.ModuleSettingsDescription;
import tech.quantit.northstar.common.model.ModuleTradeRecord;

/**
 * 模组持久化
 * @author KevinHuangwl
 *
 */
public interface IModuleRepository {

	/**
	 * 保存模组配置信息
	 * @param moduleSettingsDescription
	 */
	void saveSettings(ModuleSettingsDescription moduleSettingsDescription);
	/**
	 * 查询模组配置信息
	 * @param moduleName
	 * @return
	 */
	ModuleSettingsDescription findSettingsByName(String moduleName);
	/**
	 * 查询所有模组配置信息
	 * @return
	 */
	List<ModuleSettingsDescription> findAll();
	/**
	 * 移除模组配置信息
	 * @param moduleName
	 */
	void deleteSettingsByName(String moduleName);
	/**
	 * 保存模组运行状态信息
	 * @param moduleDescription
	 */
	void save(ModuleDescription moduleDescription);
	/**
	 * 获取模组运行状态信息
	 * @param moduleName
	 * @return
	 */
	ModuleDescription findByName(String moduleName);
	/**
	 * 移除模组运行状态信息
	 * @param moduleName
	 */
	void deleteByName(String moduleName);
	/**
	 * 保存模组成交记录
	 * @param record
	 */
	void saveTradeRecord(ModuleTradeRecord tradeRecord);
	/**
	 * 查询模组全部成交记录
	 * @param moduleName
	 * @return
	 */
	List<ModuleTradeRecord> findAllTradeRecords(String moduleName);
	/**
	 * 删除模组全部成交记录
	 * @param moduleName
	 */
	void removeAllTradeRecords(String moduleName);
	/**
	 * 保存模组交易记录
	 * @param record
	 */
	void saveDealRecord(ModuleDealRecord dealRecord);
	/**
	 * 查询模组全部交易记录
	 * @param moduleName
	 * @return
	 */
	List<ModuleDealRecord> findAllDealRecords(String moduleName);
	/**
	 * 删除模组全部交易记录
	 * @param moduleName
	 */
	void removeAllDealRecords(String moduleName);
}
