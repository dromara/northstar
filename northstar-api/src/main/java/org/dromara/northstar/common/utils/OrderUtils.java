package org.dromara.northstar.common.utils;

import org.dromara.northstar.common.model.core.Order;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

public class OrderUtils {
	
	private OrderUtils() {}
	
	public static PositionDirectionEnum getClosingDirection(DirectionEnum dir) {
		if(dir == DirectionEnum.D_Buy) {
			return PositionDirectionEnum.PD_Short;
		} else if(dir == DirectionEnum.D_Sell) {
			return PositionDirectionEnum.PD_Long;
		} 
		
		throw new IllegalArgumentException("无法确定[" + dir + "]的对应持仓方向");
	}
	
	public static DirectionEnum getClosingDirection(PositionDirectionEnum dir) {
		return switch(dir) {
		case PD_Long -> DirectionEnum.D_Sell;
		case PD_Short -> DirectionEnum.D_Buy;
		default -> throw new IllegalStateException("无法确定[" + dir + "]的对应持仓方向");
		};
	}

	public static boolean isValidOrder(Order order) {
		return order.orderStatus() != OrderStatusEnum.OS_Rejected && order.orderStatus() != OrderStatusEnum.OS_Canceled;
	}
	
	public static boolean isDoneOrder(Order order) {
		return order.orderStatus() == OrderStatusEnum.OS_Canceled || order.orderStatus() == OrderStatusEnum.OS_AllTraded;
	}
	
	public static boolean isPendingOrder(Order order) {
		return isValidOrder(order) && !isDoneOrder(order);
	}
}
