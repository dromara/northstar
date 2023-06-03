package org.dromara.northstar.strategy;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.constant.ModuleState;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;

/**
 * 模组账户代表一个模组内部的逻辑账户
 * 若模组绑定了多个实体账户，也被看作是一个逻辑账户
 * @author KevinHuangwl
 *
 */
public interface IModuleAccount extends TickDataAware, TransactionAware {
	
	/**
	 * 可用金额
	 * @return
	 */
	double availableAmount();
	/**
	 * 获取指定合约指定方向的持仓数
	 * @param unifiedSymbol 	
	 * @param direction
	 * @return
	 */
	int getNonclosedPosition(String unifiedSymbol, DirectionEnum direction);
	/**
	 * 获取指定合约的净持仓数量
	 * @param unifiedSymbol
	 * @return				正数代表净多头持仓，负数代表净空头持仓
	 */
	int getNonclosedNetPosition(String unifiedSymbol);
	/**
	 * 获取模组状态
	 * @return
	 */
	ModuleState getModuleState();
}
