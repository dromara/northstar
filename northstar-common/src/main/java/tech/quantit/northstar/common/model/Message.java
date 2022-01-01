package tech.quantit.northstar.common.model;

import java.time.LocalDateTime;

import lombok.Data;
import tech.quantit.northstar.common.utils.MessagePrinter;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

@Data
public class Message {
	
	private LocalDateTime dateTime;
	
	private String title;
	
	private String content;
	
	private String[] receivers;

	public Message(String title, String content, String[] receivers) {
		this.title = title;
		this.content = content;
		this.receivers = receivers;
		dateTime = LocalDateTime.now();
	}
	
	public Message(NoticeField notice, String[] receivers) {
		this(String.format("Northstar消息通知 - [%s]", notice.getStatus().toString()), notice.getContent(), receivers);
	}
	
	public Message(OrderField order, String[] receivers) {
		this(String.format("Northstar下单通知 - [%s]", order.getGatewayId()), MessagePrinter.print(order), receivers);
	}
	
	public Message(TradeField trade, String[] receivers) {
		this(String.format("Northstar成交通知 - [%s]", trade.getGatewayId()), MessagePrinter.print(trade), receivers);
	}
}
