package org.dromara.northstar.common.model.core;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import lombok.Builder;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.IContractManager;
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
		Contract contract  // 合约
) {

	public TradeField toTradeField() {
		return TradeField.newBuilder()
				.setGatewayId(Optional.ofNullable(gatewayId).orElse(""))
				.setOriginOrderId(Optional.ofNullable(originOrderId).orElse(""))
				.setOrderId(Optional.ofNullable(orderId).orElse(""))
				.setDirection(direction)
				.setOffsetFlag(offsetFlag)
				.setPrice(price)
				.setVolume(volume)
				.setTradeType(tradeType)
				.setPriceSource(priceSource)
				.setTradingDay(Optional.ofNullable(tradingDay).orElse(LocalDate.MIN).toString())
				.setTradeDate(Optional.ofNullable(tradeDate).orElse(LocalDate.MIN).toString())
				.setTradeTime(Optional.ofNullable(tradeTime).orElse(LocalTime.MIN).toString())
				.setTradeTimestamp(tradeTimestamp)
				.setContract(contract.toContractField())
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
				.tradingDay(LocalDate.parse(trade.getTradingDay()))
				.tradeDate(LocalDate.parse(trade.getTradeDate()))
				.tradeTime(LocalTime.parse(trade.getTradeTime()))
				.tradeTimestamp(trade.getTradeTimestamp())
				.contract(contractManager.getContract(Identifier.of(trade.getContract().getUnifiedSymbol())).contract())
				.build();
	}
}
