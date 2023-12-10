package org.dromara.northstar.common.model.core;

import java.time.LocalDate;
import java.time.LocalTime;

import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.IContractManager;

import com.alibaba.fastjson.annotation.JSONField;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreEnum.TradeTypeEnum;
import xyz.redtorch.pb.CoreField.TradeField;

@Builder(toBuilder = true)
public record Trade(
		String gatewayId,
		String originOrderId,		// 本地订单ID
		String orderId,				// 网关订单ID
		DirectionEnum direction,      // 方向
		OffsetFlagEnum offsetFlag,  // 开平
		double price,  // 价格
		int volume,  // 数量
		TradeTypeEnum tradeType, // 成交类型
		PriceSourceEnum priceSource, // 成交价来源
		LocalDate tradingDay,  // 交易日
		LocalDate tradeDate,  // 成交日期
		LocalTime tradeTime,  // 成交时间(HHmmssSSS)
		long tradeTimestamp,  // 成交时间戳
		@JSONField(serialize = false)
		Contract contract  // 合约
) {

	public TradeField toTradeField() {
		TradeField.Builder builder = TradeField.newBuilder();
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
		if (tradeType != null) {
			builder.setTradeType(tradeType);
		}
		if (priceSource != null) {
			builder.setPriceSource(priceSource);
		}
		if (tradingDay != null) {
			builder.setTradingDay(tradingDay.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		}
		if (tradeDate != null) {
			builder.setTradeDate(tradeDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		}
		if (tradeTime != null) {
			builder.setTradeTime(tradeTime.format(DateTimeConstant.T_FORMAT_FORMATTER));
		}
		if (contract != null) {
			builder.setContract(contract.toContractField());
		}

		return builder
				.setPrice(price)
				.setVolume(volume)
				.setTradeTimestamp(tradeTimestamp)
				.build();
	}

	public static Trade of(TradeField trade, IContractManager contractManager){
		return Trade.builder()
				.gatewayId(trade.getGatewayId())
				.originOrderId(trade.getOriginOrderId())
				.orderId(trade.getOrderId())
				.direction(trade.getDirection())
				.offsetFlag(trade.getOffsetFlag())
				.price(trade.getPrice())
				.volume(trade.getVolume())
				.tradeType(trade.getTradeType())
				.priceSource(trade.getPriceSource())
				.tradingDay(LocalDate.parse(trade.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.tradeDate(LocalDate.parse(trade.getTradeDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.tradeTime(LocalTime.parse(trade.getTradeTime(), DateTimeConstant.T_FORMAT_FORMATTER))
				.tradeTimestamp(trade.getTradeTimestamp())
				.contract(contractManager.getContract(Identifier.of(trade.getContract().getContractId())).contract())
				.build();
	}
}
