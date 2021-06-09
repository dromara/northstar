package tech.xuanwu.northstar.plugin.mail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tech.xuanwu.northstar.common.event.AbstractEventHandler;
import tech.xuanwu.northstar.common.event.GenericEventHandler;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.event.PluginEventBus;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

@Component
public class NotificationDispatcher extends AbstractEventHandler implements InitializingBean, GenericEventHandler{

	@Autowired
	private PluginEventBus peBus;
	
	@Autowired
	private List<MessageSender> senderList;
	
	private Set<NorthstarEventType> eventSet = new HashSet<>() {
		private static final long serialVersionUID = 1L;

		{
			add(NorthstarEventType.NOTICE);
			add(NorthstarEventType.CONNECTED);
			add(NorthstarEventType.DISCONNECTING);
			add(NorthstarEventType.ORDER);
			add(NorthstarEventType.TRADE);
		}
	};
	
	@Override
	public void afterPropertiesSet() throws Exception {
		peBus.register(this);
	}


	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return  eventSet.contains(eventType);
	}


	@Override
	protected void doHandle(NorthstarEvent e) {
		switch(e.getEvent()) {
		case NOTICE:
			handleNotice((NoticeField) e.getData());
			break;
		case CONNECTED:
			handleConnection((String) e.getData());
			break;
		case DISCONNECTING:
			handleDisconnection((String) e.getData());
			break;
		case ORDER:
			break;
		case TRADE:
			break;
		default:
			throw new IllegalArgumentException("未定义处理类型：" + e.getEvent());
		}
		
	}
	
	private void handleNotice(NoticeField notice) {
		Message msg = new Message(notice);
		for(MessageSender sender : senderList) {
			sender.send(msg);
		}
	}
	
	private void handleConnection(String gatewayId) {
		Message msg = new Message(String.format("[%s] - 连线", gatewayId), "");
		for(MessageSender sender : senderList) {
			sender.send(msg);
		}
	}
	
	private void handleDisconnection(String gatewayId) {
		Message msg = new Message(String.format("[%s] - 离线", gatewayId), "");
		for(MessageSender sender : senderList) {
			sender.send(msg);
		}
	}
	
	private void handleOrder(OrderField order) {
		
	}
	
	private void handleTrade(TradeField trade) {
		
	}

}
