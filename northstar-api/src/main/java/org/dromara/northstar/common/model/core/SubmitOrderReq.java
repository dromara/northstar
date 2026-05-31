package org.dromara.northstar.common.model.core;

import java.util.concurrent.atomic.AtomicLong;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

@Builder
public record SubmitOrderReq(
		String gatewayId,			// 交易网关ID
		String originOrderId,       	// 本地订单ID
		Contract contract,          	// 合约
		double volume,              	// 下单数量
		double price,               	// 下单价格 （当价格为0时，采用市价；否则采用限价）
		DirectionEnum direction,    // 买卖方向
		OffsetFlagEnum offsetFlag,  	// 开平标识
		double takeProfit,			// 止盈价
		double stopLoss            	// 止损价
		) {
	
	private static final AtomicLong atomic = new AtomicLong();
	public SubmitOrderReq{
		if(originOrderId == null) originOrderId = System.currentTimeMillis() + atomic.incrementAndGet() + "";
	}
	
}
