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
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
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
		String dir = order.getDirection() == DirectionEnum.D_Buy ? "多" :  order.getDirection() == DirectionEnum.D_Sell ? "空" : "无";
		String offset = order.getOffsetFlag() == OffsetFlagEnum.OF_Open ? "开" : order.getOffsetFlag() == OffsetFlagEnum.OF_Unkonwn ? "无" : "平";
		String status = order.getOrderStatus() == OrderStatusEnum.OS_AllTraded ? "全成" :
			order.getOrderStatus() == OrderStatusEnum.OS_Canceled ? "已撤" :
				order.getOrderStatus() == OrderStatusEnum.OS_Touched ? "已挂" :
					order.getOrderStatus() == OrderStatusEnum.OS_Rejected ? "拒绝" : "";
		Message msg = new Message(String.format("[%s] - 订单：%s，合约：%s %s %d手", 
				gatewayId, status, order.getContract().getName(), dir+offset, order.getTotalVolume()), 
				String.format("合约：%s\n手数：%d\n已成交：%d\n价钱：%.2f", 
						order.getContract().getName(), order.getTotalVolume(), order.getTradedVolume(), order.getPrice()));
		doSend(msg);
	}
	
	private void handleTrade(TradeField trade) {
		String gatewayId = trade.getGatewayId();
		String dir = trade.getDirection() == DirectionEnum.D_Buy ? "多" :  trade.getDirection() == DirectionEnum.D_Sell ? "空" : "无";
		String offset = trade.getOffsetFlag() == OffsetFlagEnum.OF_Open ? "开" : trade.getOffsetFlag() == OffsetFlagEnum.OF_Unkonwn ? "无" : "平";
		Message msg = new Message(String.format("[%s] - 成交合约：%s %s %d手", gatewayId, trade.getContract().getName(),
				dir+offset, trade.getVolume()),
				"");
		doSend(msg);
	}
	
	private void doSend(Message msg) {
		for(MessageSender sender : senderList) {
			sender.send(msg);
		}
	}

}
