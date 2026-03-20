package org.dromara.northstar.common;

import org.dromara.northstar.common.model.ModuleDescription;

public interface IModuleService {

	/**
	 * 增加模组
	 * @param md
	 * @return
	 * @throws Exception 
	 */
	ModuleDescription createModule(ModuleDescription md) throws Exception;
	
	/**
	 * 修改模组
	 * @param md
	 * @return
	 * @throws Exception 
	 */
	ModuleDescription modifyModule(ModuleDescription md, boolean reset) throws Exception;

	/**
	 * 删除模组
	 * @param name
	 * @return
	 */
	boolean removeModule(String name);
	
	
}
