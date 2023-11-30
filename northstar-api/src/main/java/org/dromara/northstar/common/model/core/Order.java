package org.dromara.northstar.common.model.core;

import java.time.LocalDate;
import java.time.LocalTime;

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
import xyz.redtorch.pb.CoreField.OrderField;

@Builder(toBuilder = true)
public record Order(
		String gatewayId,
		String originOrderId,
		String orderId,
		DirectionEnum direction,    // 方向
		OffsetFlagEnum offsetFlag,  // 开平
		HedgeFlagEnum hedgeFlag,    // 投机套保标识
		OrderPriceTypeEnum orderPriceType, // 定单价格类型
		OrderStatusEnum orderStatus,  // 状态
		double price,                // 价格
		int totalVolume,            // 数量
		int tradedVolume,            // 已成交数量
		TimeConditionEnum timeCondition,  // 时效
		String gtdDate,            // GTD日期
		VolumeConditionEnum volumeCondition, // 成交量类型
		int minVolume,                // 最小成交量
		ContingentConditionEnum contingentCondition, // 触发条件
		double stopPrice,            // 止损价
		ForceCloseReasonEnum forceCloseReason, // 强平原因
		LocalDate tradingDay,        // 交易日
		LocalDate updateDate,        // 更新日期
		LocalTime updateTime,        // 更新时间
		String statusMsg,            // 状态信息
		Contract contract            // 合约
) {

	public OrderField toOrderField() {
		return OrderField.newBuilder()
				.setGatewayId(gatewayId)
				.setOriginOrderId(originOrderId)
				.setOrderId(orderId)
				.setDirection(direction)
				.setOffsetFlag(offsetFlag)
				.setHedgeFlag(hedgeFlag)
				.setOrderPriceType(orderPriceType)
				.setOrderStatus(orderStatus)
				.setPrice(price)
				.setTotalVolume(totalVolume)
				.setTradedVolume(tradedVolume)
				.setTimeCondition(timeCondition)
				.setGtdDate(gtdDate)
				.setVolumeCondition(volumeCondition)
				.setMinVolume(minVolume)
				.setContingentCondition(contingentCondition)
				.setStopPrice(stopPrice)
				.setForceCloseReason(forceCloseReason)
				.setTradingDay(tradingDay.toString())
				.setStatusMsg(statusMsg)
				.setContract(contract.toContractField())
				.build();
	}
}
