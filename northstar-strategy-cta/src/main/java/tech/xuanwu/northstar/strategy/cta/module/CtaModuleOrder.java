package tech.xuanwu.northstar.strategy.cta.module;

import java.util.HashMap;
import java.util.Map;

import tech.xuanwu.northstar.strategy.common.ModuleOrder;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.OrderField;

/**
 * 用于计算模组的订单状态,让模组自身可以检测到所发出的订单状态
 * 
 * @author KevinHuangwl
 *
 */
public class CtaModuleOrder implements ModuleOrder{
	
	/**
	 * orderId --> order
	 * 只记录未决订单(即不包括废单与已撤单)
	 */
	private Map<String, OrderField> pendingOrder = new HashMap<>();

	@Override
	public void updateOrder(OrderField order) {
		if(order.getOrderStatus() == OrderStatusEnum.OS_AllTraded
				|| order.getOrderStatus() == OrderStatusEnum.OS_Rejected
				|| order.getOrderStatus() == OrderStatusEnum.OS_Canceled) {
				pendingOrder.remove(order.getOrderId());
		} else {
			pendingOrder.put(order.getOrderId(), order);
		}
	}

	@Override
	public boolean hasPendingOrder() {
		return pendingOrder.size() > 0;
	}


}
