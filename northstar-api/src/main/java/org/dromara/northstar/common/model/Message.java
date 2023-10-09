package org.dromara.northstar.common.model;

import java.time.LocalDateTime;

import org.dromara.northstar.common.utils.MessagePrinter;

import lombok.Data;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

@Data
public class Message {
	
	private LocalDateTime dateTime;
	
	private String title;
	
	private String content;
	
	public Message(String title, String content) {
		this.title = title;
		this.content = content;
		dateTime = LocalDateTime.now();
	}
	
	public Message(NoticeField notice) {
		this(String.format("Northstar消息通知 - [%s]", notice.getStatus().toString()), notice.getContent());
	}
	
	public Message(OrderField order) {
		this(String.format("Northstar下单通知 - [%s]", order.getGatewayId()), MessagePrinter.print(order));
	}
	
	public Message(TradeField trade) {
		this(String.format("Northstar成交通知 - [%s]", trade.getGatewayId()), MessagePrinter.print(trade));
	}
}
