package org.dromara.northstar.common.model;

import com.alibaba.fastjson2.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模组运行时信息
 * @author KevinHuangwl
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleRuntimeDescription {
	/**
	 * 模组名称
	 */
	private String moduleName;
	/**
	 * 模组账户描述
	 */
	private ModuleAccountRuntimeDescription moduleAccountRuntime;
	/**
	 * 模组计算状态（持久化数据）
	 */
	private JSONObject storeObject;
	
}
