package org.dromara.northstar.common.model;

import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.constant.ModuleState;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSONArray;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模组信息
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
	 * 模组启用状态
	 */
	private boolean enabled;
	/***
	 * 模组状态
	 */
	private ModuleState moduleState;
	/**
	 * 模组账户描述
	 */
	private ModuleAccountRuntimeDescription moduleAccountRuntime;
	/**
	 * 模组关联账户描述
	 */
	private List<AccountRuntimeDescription> accountRuntimes;
	/**
	 * 合约指标集
	 */
	private Map<String, List<String>> indicatorMap;
	/**
	 * 行情与指标数据
	 */
	private Map<String, JSONArray> dataMap;
	/**
	 * 模组计算状态（持久化数据）
	 */
	private JSONObject storeObject;
	/**
	 * 策略信息描述
	 */
	private List<Value> strategyInfos;
}
