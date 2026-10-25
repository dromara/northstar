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
	private String name;
	/**
	 * 模组类型
	 */
	private ModuleType type;
	/**
	 * 模组用途
	 */
	private ModuleUsage usage;
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
	 * 初始分配金额
	 */
	private double initBalance;
	/**
	 * 默认交易量（正数，允许小数）
	 */
	@Builder.Default
	private double defaultVolume = 1;
	/**
	 * 下单超价设置
	 */
	private int orderPlusTick;
	/**
	 * 模组绑定账户 
	 */
	private List<String> bindedAccounts;
	/**
	 * 模组绑定合约
	 */
	private List<ContractSimpleInfo> bindedContracts;
	/**
	 * 策略配置信息
	 */
	private StrategyMetaInfo strategySetting;
}
