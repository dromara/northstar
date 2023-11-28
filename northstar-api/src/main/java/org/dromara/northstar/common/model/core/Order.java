package org.dromara.northstar.common.model.core;

import java.time.LocalDate;

import org.dromara.northstar.gateway.Gateway;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;

@Builder
public record Order(
		Gateway gateway,
		String originOrderId,
		String orderId,
		DirectionEnum direction,  	// 方向
		OffsetFlagEnum offsetFlag,  // 开平
		HedgeFlagEnum hedgeFlag, 	// 投机套保标识
		OrderPriceTypeEnum orderPriceType, // 定单价格类型 
		OrderStatusEnum orderStatus,  // 状态
		double price,  				// 价格
		int totalVolume,  			// 数量
		int tradedVolume,  			// 已成交数量
		TimeConditionEnum timeCondition,  // 时效
		String gtdDate, 			// GTD日期
		VolumeConditionEnum volumeCondition, // 成交量类型
		int minVolume, 				// 最小成交量
		ContingentConditionEnum contingentCondition, // 触发条件
		double stopPrice, 			// 止损价
		ForceCloseReasonEnum forceCloseReason, // 强平原因
		LocalDate tradingDay,  		// 交易日
	    Contract contract  			// 合约
	) {

}
