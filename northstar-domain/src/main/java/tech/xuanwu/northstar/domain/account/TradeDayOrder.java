package tech.xuanwu.northstar.domain.account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.OrderField;

/**
 * 交易日订单
 * @author KevinHuangwl
 *
 */
public class TradeDayOrder {

	/**
	 * originOrderId --> order
	 */
	private ConcurrentHashMap<String, OrderField> orderMap = new ConcurrentHashMap<>();
	
	
	/**
	 * 更新订单信息
	 * @param order
	 */
	public void update(OrderField order) {
		orderMap.put(order.getOriginOrderId(), order);
	}
	
	/**
	 * 是否可以撤单
	 * @param originOrderId
	 * @return
	 */
	public boolean canCancelOrder(String originOrderId) {
		OrderField order = orderMap.get(originOrderId);
		if(order == null) {
			return false;
		}
		
		return order.getOrderStatus() != OrderStatusEnum.OS_AllTraded 
				&& order.getOrderStatus() != OrderStatusEnum.OS_Canceled
				&& order.getOrderStatus() != OrderStatusEnum.OS_Rejected;
	}
	
	/**
	 * 获取订单列表
	 * @return
	 */
	public List<OrderField> getOrders(){
		List<OrderField> list = new ArrayList<>(orderMap.size());
		list.addAll(orderMap.values());
		return Collections.unmodifiableList(list);
	}
	
	
	public void dailySettlement() {
		
	}
}
