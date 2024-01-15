package org.dromara.northstar.event;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.strategy.IMessageSender;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class EventNotificationHandler extends AbstractEventHandler implements GenericEventHandler, InitializingBean, DisposableBean{
	
	private IMessageSender sender;
	
	private Set<NorthstarEventType> subEvents;
	
	private ExecutorService exec;
	
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
		exec.execute(() -> sender.onEvent(e));
	}



	@Override
	public void afterPropertiesSet() throws Exception {
		exec = CommonUtils.newThreadPerTaskExecutor(getClass());
	}
	
	@Override
	public void destroy() throws Exception {
		exec.close();
	}
}
