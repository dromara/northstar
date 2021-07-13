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
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
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
			add(NorthstarEventType.DISCONNECTED);
			add(NorthstarEventType.TRADE);
			add(NorthstarEventType.ORDER);
		}
	};
	
	private Set<OrderStatusEnum> interestedOrderStatus = new HashSet<>() {
		private static final long serialVersionUID = 1L;

		{
			add(OrderStatusEnum.OS_AllTraded);
			add(OrderStatusEnum.OS_Canceled);
			add(OrderStatusEnum.OS_Rejected);
			add(OrderStatusEnum.OS_Touched);
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
		case DISCONNECTED:
			handleDisconnection((String) e.getData());
			break;
		case ORDER:
			handleOrder((OrderField) e.getData());
			break;
		case TRADE:
			handleTrade((TradeField) e.getData());
			break;
		default:
			throw new IllegalArgumentException("未定义处理类型：" + e.getEvent());
		}
		
	}
	
	private void handleNotice(NoticeField notice) {
		Message msg = new Message(notice);
		doSend(msg);
	}
	
	private void handleConnection(String gatewayId) {
		Message msg = new Message(String.format("[%s] - 连线", gatewayId), "");
		doSend(msg);
	}
	
	private void handleDisconnection(String gatewayId) {
		Message msg = new Message(String.format("[%s] - 离线", gatewayId), "");
		doSend(msg);
	}
	
	private void handleOrder(OrderField order) {
		String gatewayId = order.getGatewayId();
		//避免订单状态变化导致邮箱过多，过滤掉不感兴趣的非关键状态
		if(!interestedOrderStatus.contains(order.getOrderStatus())) {
			return;
		}
		Message msg = new Message(String.format("[%s] - 订单：%s，合约：%s %d手", 
				gatewayId, order.getOrderStatus(), order.getContract().getName(), order.getTotalVolume()), 
				String.format("合约：%s\n手数：%d\n已成交：%d\n价钱：%0.2f", 
						order.getContract().getName(), order.getTotalVolume(), order.getTradedVolume(), order.getPrice()));
		doSend(msg);
	}
	
	private void handleTrade(TradeField trade) {
		String gatewayId = trade.getGatewayId();
		Message msg = new Message(String.format("[%s] - 成交合约：%s %d手", gatewayId, trade.getContract().getName(), trade.getVolume()),
				"");
		doSend(msg);
	}
	
	private void doSend(Message msg) {
		for(MessageSender sender : senderList) {
			sender.send(msg);
		}
	}

}
