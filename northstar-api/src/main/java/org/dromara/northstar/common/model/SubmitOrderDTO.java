package org.dromara.northstar.common.model;

import org.dromara.northstar.common.constant.TradeAction;

import lombok.Data;

@Data
public class SubmitOrderDTO {

	private String gatewayId;			// 交易网关ID
	private String originOrderId;       	// 本地订单ID
	private String contractId;          	// 合约
	private double volume;              	// 下单数量
	private double price;               	// 下单价格 （当价格为0时，采用市价；否则采用限价）
	private TradeAction action;    		// 买卖方向
	private double takeProfit;			// 止盈价
	private double stopLoss;            	// 止损价
	
}
