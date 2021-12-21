package tech.quantit.northstar.main.notification;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.event.AbstractEventHandler;
import tech.quantit.northstar.common.event.GenericEventHandler;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.utils.FieldUtils;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

@Component
public class NotificationDispatcher extends AbstractEventHandler implements GenericEventHandler{

	
	@Autowired
	private List<MessageSender> senderList;
	
	private Set<NorthstarEventType> eventSet = new HashSet<>() {
		private static final long serialVersionUID = 1L;

		{
			add(NorthstarEventType.NOTICE);
			add(NorthstarEventType.CONNECTED);
			add(NorthstarEventType.DISCONNECTED);
			add(NorthstarEventType.GATEWAY_READY);
			add(NorthstarEventType.ORDER);
			add(NorthstarEventType.TRADE);
		}
	};
	
	private Set<OrderStatusEnum> interestedOrderStatus = new HashSet<>() {
		private static final long serialVersionUID = 1L;

		{
			add(OrderStatusEnum.OS_Canceled);
			add(OrderStatusEnum.OS_Rejected);
			add(OrderStatusEnum.OS_Touched);
			add(OrderStatusEnum.OS_Unknown);
		}
	};
	
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
		case GATEWAY_READY:
			handleReady((String) e.getData());
			break;
		case ORDER:
			handleOrder((OrderField) e.getData());
			break;
		case TRADE:
			handleTrade((TradeField)e.getData());
			break;
		default:
			throw new IllegalArgumentException("未定义处理类型：" + e.getEvent());
		}
		
	}
	
	private void handleReady(String gatewayId) {
		Message msg = new Message(String.format("[%s] - 账户就绪", gatewayId), "");
		doSend(msg);
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
		String dir = FieldUtils.chn(order.getDirection());
		String offset = FieldUtils.chn(order.getOffsetFlag());
		String status = FieldUtils.chn(order.getOrderStatus());
		String content = String.format("合约：%s%n手数：%d%n已成交：%d%n价钱：%.2f%n时间：%s", 
				order.getContract().getName(), order.getTotalVolume(), order.getTradedVolume(), order.getPrice(), LocalDateTime.now().format(DateTimeConstant.DT_FORMAT_FORMATTER));
		Message msg = new Message(String.format("[%s]订单：%s，%s %s %d手", 
				gatewayId, status, order.getContract().getName(), dir+offset, order.getTotalVolume()), content);
		doSend(msg);
	}
	
	private void handleTrade(TradeField trade) {
		String dir = FieldUtils.chn(trade.getDirection());
		String offset = FieldUtils.chn(trade.getOffsetFlag());
		String content = String.format("合约：%s%n手数：%d%n价钱：%.2f%n时间：%s", 
				trade.getContract().getName(), trade.getVolume(), trade.getPrice(), LocalDateTime.now().format(DateTimeConstant.DT_FORMAT_FORMATTER));
		Message msg = new Message(String.format("[%s]成交：%s，%s %d手", 
				trade.getGatewayId(), trade.getContract().getName(), dir+offset, trade.getVolume()), content);
		doSend(msg);
	}
	
	private void doSend(Message msg) {
		for(MessageSender sender : senderList) {
			sender.send(msg);
		}
	}

}
