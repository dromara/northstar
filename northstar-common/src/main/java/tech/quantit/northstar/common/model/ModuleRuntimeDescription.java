package tech.quantit.northstar.common.model;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSONArray;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.constant.ModuleState;

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
	private Map<String, ModuleAccountRuntimeDescription> accountRuntimeDescriptionMap;
	/**
	 * 合约指标集
	 */
	private Map<String, List<String>> indicatorMap;
	/**
	 * 行情与指标数据
	 */
	private Map<String, JSONArray> dataMap;
	/**
	 * 模组计算状态（非指标化数据）
	 */
	private JSONObject dataState;
}
