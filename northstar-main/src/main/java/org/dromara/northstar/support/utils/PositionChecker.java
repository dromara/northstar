package org.dromara.northstar.support.utils;

import org.dromara.northstar.module.ModuleManager;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.PositionField;

/**
 * 持仓检查器
 * @author KevinHuangwl
 *
 */
public class PositionChecker {
	
	private ModuleManager moduleMgr;

	public PositionChecker(ModuleManager moduleMgr) {
		this.moduleMgr = moduleMgr;
	}
	
	/**
	 * 检查持仓相等
	 * @param position	某合约某个方向的物理持仓
	 */
	public void checkPositionEquivalence(PositionField position) {
		DirectionEnum direction = switch(position.getPositionDirection()) {
		case PD_Long -> DirectionEnum.D_Buy;
		case PD_Short -> DirectionEnum.D_Sell;
		default -> throw new IllegalArgumentException("Unexpected value: " + position.getPositionDirection());
		};
		int totalPosition = moduleMgr.allModules().stream()
			.mapToInt(m -> m.getModuleContext().getModuleAccount().getNonclosedPosition(position.getContract().getUnifiedSymbol(), direction))
			.sum();
		if(totalPosition != position.getPosition()) {
			throw new IllegalStateException(String.format("[%s] 逻辑持仓与实际持仓不一致。逻辑持仓数：%d，实际持仓数：%d", position.getContract().getName(), totalPosition, position.getPosition()));
		}
	}
}
