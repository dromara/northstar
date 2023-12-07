package org.dromara.northstar.common.model.core;

import java.time.LocalDate;
import java.time.LocalTime;

import org.dromara.northstar.common.constant.DateTimeConstant;

import com.alibaba.fastjson.annotation.JSONField;

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
		String originOrderId,		// 本地订单ID
		String orderId,				// 网关订单ID
		DirectionEnum direction,    // 方向
		OffsetFlagEnum offsetFlag,  // 开平
		HedgeFlagEnum hedgeFlag,    // 投机套保标识
		OrderPriceTypeEnum orderPriceType, // 定单价格类型
		OrderStatusEnum orderStatus,  // 状态
		double price,               // 价格
		int totalVolume,            // 数量
		int tradedVolume,           // 已成交数量
		TimeConditionEnum timeCondition,  // 时效
		String gtdDate,            	// GTD日期
		VolumeConditionEnum volumeCondition, // 成交量类型
		int minVolume,              // 最小成交量
		ContingentConditionEnum contingentCondition, // 触发条件
		double stopPrice,           // 止损价
		ForceCloseReasonEnum forceCloseReason, // 强平原因
		LocalDate tradingDay,       // 交易日
		LocalDate orderDate,		// 下单日期
		LocalTime orderTime,		// 下单时间
		LocalDate updateDate,       // 更新日期
		LocalTime updateTime,       // 更新时间
		String statusMsg,           // 状态信息
		@JSONField(serialize = false)
		Contract contract           // 合约
) {

	public OrderField toOrderField() {
		OrderField.Builder builder = OrderField.newBuilder();
		if (gatewayId != null) {
			builder.setGatewayId(gatewayId);
		}
		if (originOrderId != null) {
			builder.setOriginOrderId(originOrderId);
		}
		if (orderId != null) {
			builder.setOrderId(orderId);
		}
		if (direction != null) {
			builder.setDirection(direction);
		}
		if (offsetFlag != null) {
			builder.setOffsetFlag(offsetFlag);
		}
		if (hedgeFlag != null) {
			builder.setHedgeFlag(hedgeFlag);
		}
		if (orderPriceType != null) {
			builder.setOrderPriceType(orderPriceType);
		}
		if (orderStatus != null) {
			builder.setOrderStatus(orderStatus);
		}
		if (timeCondition != null) {
			builder.setTimeCondition(timeCondition);
		}
		if (gtdDate != null) {
			builder.setGtdDate(gtdDate);
		}
		if (volumeCondition != null) {
			builder.setVolumeCondition(volumeCondition);
		}
		if (contingentCondition != null) {
			builder.setContingentCondition(contingentCondition);
		}
		if (forceCloseReason != null) {
			builder.setForceCloseReason(forceCloseReason);
		}
		if (tradingDay != null) {
			builder.setTradingDay(tradingDay.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		}
		if (updateDate != null) {
			builder.setOrderDate(updateDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		}
		if (updateTime != null) {
			builder.setUpdateTime(updateTime.format(DateTimeConstant.T_FORMAT_FORMATTER));
		}
		if (statusMsg != null) {
			builder.setStatusMsg(statusMsg);
		}
		if (contract != null) {
			builder.setContract(contract.toContractField());
		}

		return builder
				.setPrice(price)
				.setTotalVolume(totalVolume)
				.setTradedVolume(tradedVolume)
				.setMinVolume(minVolume)
				.setStopPrice(stopPrice)
				.build();
	}

}
