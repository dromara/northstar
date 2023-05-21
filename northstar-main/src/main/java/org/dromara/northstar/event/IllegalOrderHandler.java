package org.dromara.northstar.event;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;

import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.OrderField;

/**
 * 废单收集器
 * 专门收集废单，用于风控警报
 * @author KevinHuangwl
 *
 */
public class IllegalOrderHandler extends AbstractEventHandler implements GenericEventHandler{
	
	private Queue<OrderField> records = new LinkedList<>();

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventType == NorthstarEventType.ORDER;
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		OrderField order = (OrderField) e.getData();
		if(!records.isEmpty() && !StringUtils.equals(records.peek().getTradingDay(), order.getTradingDay())) {
			records.clear(); 	// 每日清零
		}
		if(order.getOrderStatus() == OrderStatusEnum.OS_Rejected) {
			records.add(order);
		}
	}
	
	public Collection<OrderField> getIllegalOrders(){
		return records;
	}
}
