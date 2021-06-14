package tech.xuanwu.northstar.strategy.common;

import java.util.List;

import xyz.redtorch.pb.CoreField.OrderField;

public interface ModuleOrder {

	/**
	 * 更新订单
	 * @param order
	 */
	void updateOrder(OrderField order);
	
	/**
	 * 获取未决订单
	 * @return
	 */
	List<OrderField> getPendingOrder();
}
