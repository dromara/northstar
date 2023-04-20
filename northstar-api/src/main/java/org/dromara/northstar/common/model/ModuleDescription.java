package org.dromara.northstar.common.model;

import java.util.List;

import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.constant.ModuleUsage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模组配置信息
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
	 * 模组类型
	 */
	private ModuleType type;
	/**
	 * 模组用途
	 */
	private ModuleUsage usage;
	/**
	 * K线周期数
	 */
	private int numOfMinPerBar;
	/**
	 * 预热数据量（单位：周）
	 */
	private int weeksOfDataForPreparation;
	/**
	 * 模组缓存的数据量（模组运行状态可观察的数据量）
	 */
	private int moduleCacheDataSize;
	/**
	 * 平仓优化策略
	 */
	private ClosingPolicy closingPolicy;
	/**
	 * 模组账户配置信息 
	 */
	private List<ModuleAccountDescription> moduleAccountSettingsDescription;
	/**
	 * 策略配置信息
	 */
	private ComponentAndParamsPair strategySetting;
}
