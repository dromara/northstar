package org.dromara.northstar.event;

import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.strategy.IMessageSender;

public class EventNotificationHandler extends AbstractEventHandler implements GenericEventHandler{
	
	private IMessageSender sender;
	
	private Set<NorthstarEventType> subEvents;
	
	public EventNotificationHandler(IMessageSender sender, Set<NorthstarEventType> subEvents) {
		this.sender = sender;
		this.subEvents = subEvents;
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return subEvents.contains(eventType);
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		if(Objects.isNull(sender) 
				|| e.getData() instanceof Trade trade && StringUtils.isBlank(trade.originOrderId())
				|| e.getData() instanceof Order order && (StringUtils.isBlank(order.originOrderId()) || !OrderUtils.isValidOrder(order))) {
			return;
		}
		sender.onEvent(e);
	}

}
