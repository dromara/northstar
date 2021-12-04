package tech.xuanwu.northstar.main.notification;

import java.time.LocalDateTime;

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
		dateTime = LocalDateTime.now();
		content = notice.getContent();
		title = String.format("Northstar消息通知 - [%s]", notice.getStatus().toString());
	}
	
	public Message(OrderField order) {
		dateTime = LocalDateTime.now();
		content = order.toString();
		title = String.format("Northstar下单通知 - [%s]", order.getGatewayId());
	}
	
	public Message(TradeField trade) {
		dateTime = LocalDateTime.now();
		content = trade.toString();
		title = String.format("Northstar成交通知 - [%s]", trade.getGatewayId());
	}
}
