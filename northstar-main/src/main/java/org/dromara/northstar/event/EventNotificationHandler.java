package org.dromara.northstar.event;

import java.util.Objects;
import java.util.Set;

import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.strategy.IMessageSender;

import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

public class EventNotificationHandler extends AbstractEventHandler implements GenericEventHandler{
	
	private static final int ONE_MIN = 60000;

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
				|| e.getData() instanceof TradeField trade && Math.abs(System.currentTimeMillis() - trade.getTradeTimestamp()) > ONE_MIN
				|| e.getData() instanceof OrderField order && !OrderUtils.isValidOrder(order)) {
			return;
		}
		sender.onEvent(e);
	}

}
