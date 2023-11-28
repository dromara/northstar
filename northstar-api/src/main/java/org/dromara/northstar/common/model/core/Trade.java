package org.dromara.northstar.common.model.core;

import java.time.LocalDate;
import java.time.LocalTime;

import org.dromara.northstar.gateway.Gateway;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreEnum.TradeTypeEnum;

@Builder
public record Trade(
		Gateway gateway,
		String originOrderId,
		String orderId,
		DirectionEnum direction,  	// 方向
		OffsetFlagEnum offsetFlag,  // 开平
		HedgeFlagEnum hedgeFlag, // 投机套保标识
		double price,  // 价格
		int volume,  // 数量
		TradeTypeEnum tradeType, // 成交类型
		PriceSourceEnum priceSource, // 成交价来源
		LocalDate tradingDay,  // 交易日
		LocalDate tradeDate,  // 成交日期
		LocalTime tradeTime,  // 成交时间(HHmmssSSS)
		long tradeTimestamp,  // 成交时间戳
	    Contract contract  // 合约
	) {

}
