package org.dromara.northstar.event;

import java.util.Arrays;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.core.Notice;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.support.utils.MessageSenderFacade;

public class EventNotificationHandler extends AbstractEventHandler implements GenericEventHandler{
	
	private MessageSenderFacade sender;
	
	private final Set<NorthstarEventType> subEvents = Set.of(
			NorthstarEventType.LOGGED_IN, 
			NorthstarEventType.LOGGED_OUT, 
			NorthstarEventType.TRADE
	);
	
	public EventNotificationHandler(MessageSenderFacade sender) {
		this.sender = sender;
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return subEvents.contains(eventType);
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		if(e.getData() instanceof Trade trade && StringUtils.isBlank(trade.originOrderId())
		|| e.getData() instanceof Order order && (StringUtils.isBlank(order.originOrderId()) || !OrderUtils.isValidOrder(order))) {
			return;
		}
		String title = "";
		StringBuilder sb = new StringBuilder();
		switch(e.getEvent()) {
			case LOGGED_IN -> {
				title = "网关连线";
				sb.append(String.format("【%s】 已连线", e.getData()));
			}
			case LOGGED_OUT -> {
				title = "网关离线";
				sb.append(String.format("【%s】 已离线", e.getData()));
			}
			case NOTICE -> {
				title = "系统消息";
				Notice notice = (Notice) e.getData();
				String msgLevel = switch(notice.status()) {
					case COMS_ERROR -> "错误";
					case COMS_INFO -> "提示";
					case COMS_SUCCESS -> "成功反馈";
					case COMS_WARN -> "警告";
					default -> throw new IllegalStateException("未定义类型：" + notice.status());
				};
				sb.append("消息级别：" + msgLevel + ":\n");
				sb.append(notice.content());
			}
			case ORDER -> {
				title = "订单反馈";
				Order order = (Order) e.getData();
				sb.append(String.format("交易品种：%s%n", order.contract().fullName()));
				sb.append(String.format("操作：%s%n", FieldUtils.chn(order.direction()) + FieldUtils.chn(order.offsetFlag())));
				sb.append(String.format("委托价：%s%n", order.price()));
				sb.append(String.format("交易量：%s%n", order.totalVolume()));
				sb.append(String.format("订单状态：%s%n", FieldUtils.chn(order.orderStatus())));
				sb.append(String.format("已成交：%s%n", order.tradedVolume()));
				sb.append(String.format("账户：%s%n", order.gatewayId()));
				sb.append(String.format("委托ID：%s%n", order.originOrderId()));
			}
			case TRADE -> {
				title = "成交反馈";
				Trade trade = (Trade) e.getData();
				sb.append(String.format("交易品种：%s%n", trade.contract().fullName()));
				sb.append(String.format("操作：%s%n", FieldUtils.chn(trade.direction()) + FieldUtils.chn(trade.offsetFlag())));
				sb.append(String.format("成交价：%s%n", trade.price()));
				sb.append(String.format("交易量：%s%n", trade.volume()));
				sb.append(String.format("成交时间：%s%n", trade.tradeTime()));
				sb.append(String.format("账户：%s%n", trade.gatewayId()));
				sb.append(String.format("委托ID：%s%n", trade.originOrderId()));
			}
			default -> throw new IllegalStateException("未定义处理方案：" + e.getEvent());
		}

		sender.send(title, sb.toString());
	}
	
	public Set<NorthstarEventType> subcribedEvents(){
		return subEvents;
	}
	
	public void subscribe(NorthstarEventType ...event) {
		subEvents.addAll(Arrays.asList(event));
	}
	
}
