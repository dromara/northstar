package org.dromara.northstar.event;

import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.strategy.IMessageSender;

import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

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
				|| e.getData() instanceof TradeField trade && StringUtils.isBlank(trade.getOriginOrderId())
				|| e.getData() instanceof OrderField order && (StringUtils.isBlank(order.getOriginOrderId()) || !OrderUtils.isValidOrder(order))) {
			return;
		}
		sender.onEvent(e);
	}

}
