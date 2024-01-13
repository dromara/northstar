package org.dromara.northstar.common.model.core;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;

import org.dromara.northstar.common.constant.DateTimeConstant;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreEnum.TradeTypeEnum;
import xyz.redtorch.pb.CoreField.TradeField;

class TradeTest {

	@Test
	void testToTradeField() {
		Trade trade = Trade.builder()
				.gatewayId("testGatewayId")
				.contract(Contract.builder()
						.unifiedSymbol("testSymbol")
						.symbol("testSymbol")
						.build())
				.originOrderId("testTradeId")
				.orderId("testOrderId")
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(1.0)
				.volume(2)
				.tradeType(TradeTypeEnum.TT_Common)
				.priceSource(PriceSourceEnum.PSRC_Buy)
				.tradingDay(LocalDate.now())
				.tradeDate(LocalDate.now())
				.tradeTime(LocalTime.now())
				.tradeTimestamp(System.currentTimeMillis())
				.build();
		
		TradeField tradeField = trade.toTradeField();
		assertEquals(trade.gatewayId(), tradeField.getGatewayId());
		assertEquals(trade.originOrderId(), tradeField.getOriginOrderId());
		assertEquals(trade.orderId(), tradeField.getOrderId());
		assertEquals(trade.direction(), tradeField.getDirection());
		assertEquals(trade.offsetFlag(), tradeField.getOffsetFlag());
		assertEquals(trade.price(), tradeField.getPrice());
		assertEquals(trade.volume(), tradeField.getVolume());
		assertEquals(trade.tradeType(), tradeField.getTradeType());
		assertEquals(trade.priceSource(), tradeField.getPriceSource());
		assertEquals(trade.tradingDay().format(DateTimeConstant.D_FORMAT_INT_FORMATTER), tradeField.getTradingDay());
		assertEquals(trade.tradeDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER), tradeField.getTradeDate());
		assertEquals(trade.tradeTime().format(DateTimeConstant.T_FORMAT_FORMATTER), tradeField.getTradeTime());
		assertEquals(trade.tradeTimestamp(), tradeField.getTradeTimestamp());

	}

}
