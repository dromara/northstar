package org.dromara.northstar.event;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.core.Order;

import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;

/**
 * 废单收集器
 * 专门收集废单，用于风控警报
 * @author KevinHuangwl
 *
 */
public class IllegalOrderHandler extends AbstractEventHandler implements GenericEventHandler{
	
	private Queue<Order> records = new LinkedList<>();

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventType == NorthstarEventType.ORDER;
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		Order order = (Order) e.getData();
		if(!records.isEmpty() && !records.peek().tradingDay().equals(order.tradingDay())) {
			records.clear(); 	// 每日清零
		}
		if(order.orderStatus() == OrderStatusEnum.OS_Rejected) {
			records.add(order);
		}
	}
	
	public Collection<Order> getIllegalOrders(){
		return records;
	}
}
