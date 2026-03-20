package org.dromara.northstar.data;

import java.util.List;

import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;

/**
 * 模组持久化
 * @author KevinHuangwl
 *
 */
public interface IModuleRepository {

	/**
	 * 保存模组配置信息
	 * @param moduleDescription
	 */
	void saveSettings(ModuleDescription moduleDescription);
	/**
	 * 查询模组配置信息
	 * @param moduleName
	 * @return
	 */
	ModuleDescription findSettingsByName(String moduleName);
	/**
	 * 查询所有模组配置信息
	 * @return
	 */
	List<ModuleDescription> findAllSettings();
	/**
	 * 移除模组配置信息
	 * @param moduleName
	 */
	void deleteSettingsByName(String moduleName);
	/**
	 * 保存模组运行状态信息
	 * @param moduleRtDescription
	 */
	void saveRuntime(ModuleRuntimeDescription moduleRtDescription);
	/**
	 * 获取模组运行状态信息
	 * @param moduleName
	 * @return
	 */
	ModuleRuntimeDescription findRuntimeByName(String moduleName);
	/**
	 * 移除模组运行状态信息
	 * @param moduleName
	 */
	void deleteRuntimeByName(String moduleName);
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
