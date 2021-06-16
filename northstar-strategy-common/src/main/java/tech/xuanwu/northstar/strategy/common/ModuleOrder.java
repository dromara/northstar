package tech.xuanwu.northstar.strategy.common;

import xyz.redtorch.pb.CoreField.OrderField;

/**
 * 用于计算模组的订单状态,让模组自身可以检测到所发出的订单状态
 * @author KevinHuangwl
 *
 */
public interface ModuleOrder {

	/**
	 * 更新订单
	 * @param order
	 */
	void updateOrder(OrderField order);
	
//	/**
//	 * 获取未决订单
//	 * @return
//	 */
//	List<OrderField> getPendingOrder();
	
	/**
	 * 是否有未决订单
	 * @return
	 */
	boolean hasPendingOrder();
}
