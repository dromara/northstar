package tech.quantit.northstar.common;

import tech.quantit.northstar.common.utils.FieldUtils;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 邮件内容生成器
 * 接收到不同的事件时所构建的邮件内容
 * @author KevinHuangwl
 *
 */
public interface IMailMessageContentHandler {

	default String onEvent(TradeField trade) {
		StringBuilder sb = new StringBuilder();
		sb.append("[账户成交]:\n");
		sb.append(String.format("交易品种：%s%n", trade.getContract().getFullName()));
		sb.append(String.format("操作：%s%n", FieldUtils.chn(trade.getDirection()) + FieldUtils.chn(trade.getOffsetFlag())));
		sb.append(String.format("成交价：%s%n", trade.getPrice()));
		sb.append(String.format("手数：%s%n", trade.getVolume()));
		sb.append(String.format("成交时间：%s%n", trade.getTradeTime()));
		sb.append(String.format("账户：%s%n", trade.getGatewayId()));
		sb.append(String.format("委托ID：%s%n", trade.getOriginOrderId()));
		return sb.toString();
	}
	
	default String onEvent(OrderField order) {
		StringBuilder sb = new StringBuilder();
		sb.append("[账户订单]:\n");
		sb.append(String.format("交易品种：%s%n", order.getContract().getFullName()));
		sb.append(String.format("操作：%s%n", FieldUtils.chn(order.getDirection()) + FieldUtils.chn(order.getOffsetFlag())));
		sb.append(String.format("委托价：%s%n", order.getPrice()));
		sb.append(String.format("手数：%s%n", order.getTotalVolume()));
		sb.append(String.format("订单状态：%s%n", FieldUtils.chn(order.getOrderStatus())));
		sb.append(String.format("已成交：%s%n", order.getTradedVolume()));
		sb.append(String.format("账户：%s%n", order.getGatewayId()));
		sb.append(String.format("委托ID：%s%n", order.getOriginOrderId()));
		return sb.toString();
	}
	
	default String onEvent(NoticeField notice) {
		StringBuilder sb = new StringBuilder();
		sb.append("[系统消息]:\n");
		sb.append(notice.getContent());
		sb.append(String.format("消息级别：%s%n", notice.getStatus()));
		return sb.toString();
	}
	
	default String onConnected(String gatewayId) {
		StringBuilder sb = new StringBuilder();
		sb.append("[网关连线]:\n");
		sb.append(String.format("【%s】 已连线", gatewayId));
		return sb.toString();
	}
	
	default String onDisconnected(String gatewayId) {
		StringBuilder sb = new StringBuilder();
		sb.append("[网关离线]:\n");
		sb.append(String.format("【%s】 已离线", gatewayId));
		return sb.toString();
	}
	
}
