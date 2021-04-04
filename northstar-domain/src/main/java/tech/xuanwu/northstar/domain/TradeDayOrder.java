package tech.xuanwu.northstar.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.OrderField;

/**
 * 交易日订单
 * @author KevinHuangwl
 *
 */
public class TradeDayOrder {

	private ConcurrentHashMap<String, OrderField> orderMap = new ConcurrentHashMap<>();
	
	private ConcurrentLinkedQueue<String> orderQ = new ConcurrentLinkedQueue<>();
	
	
	/**
	 * 更新订单信息
	 * @param order
	 */
	public void update(OrderField order) {
		if(!orderMap.containsKey(order.getOrderId())) {
			orderQ.offer(order.getOrderId());
		}
		orderMap.put(order.getOrderId(), order);
	}
	
	/**
	 * 是否可以撤单
	 * @param orderId
	 * @return
	 */
	public boolean canCancelOrder(String orderId) {
		OrderField order = orderMap.get(orderId);
		if(order == null) {
			throw new NoSuchElementException("不存在相关订单：" + orderId);
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
		List<OrderField> result = new ArrayList<>(orderQ.size());
		orderQ.stream().forEach(id -> result.add(orderMap.get(id)));
		return result;
	}
}
