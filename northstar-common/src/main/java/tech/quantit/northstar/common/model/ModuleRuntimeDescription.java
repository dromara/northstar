package tech.quantit.northstar.common.model;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

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
	 * 指标数据
	 */
	private Map<String, IndicatorData> indicatorMap;
	/**
	 * 行情数据
	 */
	private Map<String, List<byte[]>> barDataMap;
	/**
	 * 模组计算状态
	 */
	private JSONObject dataState;
}
