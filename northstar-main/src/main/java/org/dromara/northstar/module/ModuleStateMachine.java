package org.dromara.northstar.module;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.strategy.IModuleContext;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;

/**
 * 模组状态机
 * 负责计算模组状态
 * @author KevinHuangwl
 *
 */
public class ModuleStateMachine implements TransactionAware {
	
	private ModuleState curState = ModuleState.EMPTY;
	
	private ModuleAccount moduleAccount;
	
	private IModuleContext ctx;
	
	private boolean shouldUpdateState;
	
	public ModuleStateMachine(IModuleContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public void onOrder(Order order) {
		ctx.getLogger().info("收到订单反馈：{} {} {}", order.originOrderId(), order.orderStatus(), order.statusMsg());
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
		List<Trade> nonclosedTrade = moduleAccount.getNonclosedTrades();
		if(nonclosedTrade.isEmpty()) {
			setState(ModuleState.EMPTY);
		} else {
			int buyPos = nonclosedTrade.stream().filter(t -> t.direction() == DirectionEnum.D_Buy).mapToInt(Trade::volume).sum();
			int sellPos = nonclosedTrade.stream().filter(t -> t.direction() == DirectionEnum.D_Sell).mapToInt(Trade::volume).sum();
			if(buyPos * sellPos > 0) {
				Trade longTrade = nonclosedTrade.stream().filter(t -> t.direction() == DirectionEnum.D_Buy).toList().get(0);
				Trade shortTrade = nonclosedTrade.stream().filter(t -> t.direction() == DirectionEnum.D_Sell).toList().get(0);
				if(longTrade.contract().equals(shortTrade.contract()) && buyPos == sellPos) {
					setState(ModuleState.EMPTY_HEDGE);
				} else {
					setState(ModuleState.HOLDING_HEDGE);
				}
			} else if(buyPos > 0) {
				setState(ModuleState.HOLDING_LONG);
			} else if(sellPos > 0) {
				setState(ModuleState.HOLDING_SHORT);
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
		ctx.getLogger().info("状态机切换：[{}] => [{}]", curState, newState);
		this.curState = newState;
	}

	public void onSubmitReq() {
		if(curState.isOrdering()) {
			throw new IllegalStateException(String.format("当前状态：%s，不能继续下单", curState));
		}
		setState(ModuleState.PLACING_ORDER);
	}

	public void setModuleAccount(ModuleAccount moduleAccount) {
		this.moduleAccount = moduleAccount;
		updateState();
	}

}
