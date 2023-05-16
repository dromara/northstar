package org.dromara.northstar.event;

import java.util.EnumSet;
import java.util.Set;

import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.support.notification.MailDeliveryManager;

import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

public class MailBindedEventHandler extends AbstractEventHandler implements GenericEventHandler{
	
	private static final int ONE_MIN = 60000;

	private MailDeliveryManager mailMgr;
	
	private static final Set<NorthstarEventType> TARGET_TYPE = EnumSet.of(
			NorthstarEventType.LOGGED_IN,
			NorthstarEventType.LOGGED_OUT,
			NorthstarEventType.NOTICE,
			NorthstarEventType.TRADE,
			NorthstarEventType.ORDER
	); 
	
	public MailBindedEventHandler(MailDeliveryManager mailMgr) {
		this.mailMgr = mailMgr;
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return TARGET_TYPE.contains(eventType);
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		if(e.getData() instanceof TradeField trade && Math.abs(System.currentTimeMillis() - trade.getTradeTimestamp()) > ONE_MIN
				|| e.getData() instanceof OrderField order && !OrderUtils.isValidOrder(order)) {
			return;
		}
		mailMgr.onEvent(e);
	}

}
