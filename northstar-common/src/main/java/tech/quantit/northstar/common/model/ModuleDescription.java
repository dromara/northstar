package tech.quantit.northstar.common.model;

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
public class ModuleDescription {
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
	private Map<String, ModuleAccountDescription> accountDescriptions;
	/**
	 * 模组计算状态
	 */
	private JSONObject dataState;
}
