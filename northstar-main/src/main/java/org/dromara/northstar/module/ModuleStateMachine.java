package org.dromara.northstar.module;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.ModuleLoggerAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.common.utils.OrderUtils;
import org.slf4j.Logger;

import lombok.Setter;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * 模组状态机
 * 负责计算模组状态
 * @author KevinHuangwl
 *
 */
public class ModuleStateMachine implements TransactionAware, ModuleLoggerAware {
	
	private ModuleState curState = ModuleState.EMPTY;
	
	private ModuleState stagingState;
	
	private ModuleAccount moduleAccount;

	@Setter
	private Logger logger;
	
	private boolean shouldUpdateState;
	
	@Override
	public void onOrder(Order order) {
		if(order.orderStatus() == OrderStatusEnum.OS_Rejected || order.orderStatus() == OrderStatusEnum.OS_Canceled) {
			updateState();
		} else if(order.orderStatus() == OrderStatusEnum.OS_AllTraded) {
			shouldUpdateState = true;
		} else if(OrderUtils.isValidOrder(order)) {
			setState(ModuleState.PENDING_ORDER);
		} else {
			throw new IllegalStateException(String.format("当前状态异常：%s，收到订单：%s %s", curState, order.orderStatus(), order.statusMsg()));
		}
	}

	@Override
	public void onTrade(Trade trade) {
		if(trade.direction() == DirectionEnum.D_Unknown) {
			throw new IllegalArgumentException("成交方向不明确");
		}
		if(trade.offsetFlag() == OffsetFlagEnum.OF_Unknown) {
			throw new IllegalArgumentException("操作意图不明确");
		}
		if(shouldUpdateState || StringUtils.equals(trade.originOrderId(), Constants.MOCK_ORDER_ID)) {
			updateState();
			shouldUpdateState = false;
		}
	}
	
	public void updateState() {
		List<Position> positions = moduleAccount.getPositions().stream().filter(p -> p.position() > 0).toList();
		List<Position> longPos = positions.stream().filter(p -> p.position() > 0 && p.positionDirection() == PositionDirectionEnum.PD_Long).toList();
		List<Position> shortPos = positions.stream().filter(p -> p.position() > 0 && p.positionDirection() == PositionDirectionEnum.PD_Short).toList();
		if(positions.isEmpty()) {
			setState(ModuleState.EMPTY);
		} else if (longPos.isEmpty() && !shortPos.isEmpty()){
			setState(ModuleState.HOLDING_SHORT);
		} else if (!longPos.isEmpty() && shortPos.isEmpty()){
			setState(ModuleState.HOLDING_LONG);
		} else {
			double buyPos = longPos.stream().mapToDouble(Position::position).sum();
			double sellPos = shortPos.stream().mapToDouble(Position::position).sum();
			Position lp = longPos.getFirst();
			Position sp = shortPos.getFirst();
			if(CommonUtils.equals(sellPos, buyPos, Math.pow(1, -lp.contract().quantityPrecision()))) {
				if(lp.contract().equals(sp.contract())) {
					setState(ModuleState.EMPTY_HEDGE);
				} else {
					setState(ModuleState.HOLDING_HEDGE);
				}
			} else {
				setState(ModuleState.ERROR);
			}
		}
	}
	
	public ModuleState getState() {
		return curState;
	}
	
	private void setState(ModuleState newState) {
		if(curState == newState) {
			return;
		}
		logger.info("状态机切换：[{}] => [{}]", curState, newState);
		this.curState = newState;
	}

	public void onSubmitReq() {
		if(curState.isOrdering()) {
			throw new IllegalStateException(String.format("当前状态：%s，不能继续下单", curState));
		}
		stagingState = curState;
		setState(ModuleState.PLACING_ORDER);
	}
	
	public void onFailSubmitReq() {
		setState(stagingState);
	}

	public void setModuleAccount(ModuleAccount moduleAccount) {
		this.moduleAccount = moduleAccount;
		updateState();
	}

}
