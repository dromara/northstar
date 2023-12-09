package org.dromara.northstar.common.model.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.junit.jupiter.api.Test;

class BarTest {

	@Test
	void testToBarField() {
		Bar bar = Bar.builder()
				.gatewayId("gatewayId")
				.contract(Contract.builder().unifiedSymbol("test").build())
				.actionDay(LocalDate.now())
				.actionTime(LocalTime.now())
				.tradingDay(LocalDate.now())
				.actionTimestamp(1L)
				.closePrice(1.0)
				.highPrice(2.0)
				.lowPrice(3.0)
				.openPrice(4.0)
				.turnoverDelta(1.0)
				.turnover(2.0)
				.volume(5)
				.volumeDelta(6)
				.openInterest(7.0)
				.openInterestDelta(8.0)
				.preSettlePrice(9.0)
				.preOpenInterest(10.0)
				.preSettlePrice(11.0)
				.channelType(ChannelType.BIAN)
				.build();
		xyz.redtorch.pb.CoreField.BarField barField = bar.toBarField();
		assertEquals(bar.gatewayId(), barField.getGatewayId());
		assertEquals(bar.actionDay().format(DateTimeConstant.D_FORMAT_INT_FORMATTER), barField.getActionDay());
		assertEquals(bar.tradingDay().format(DateTimeConstant.D_FORMAT_INT_FORMATTER), barField.getTradingDay());
		assertEquals(bar.actionTime().format(DateTimeConstant.T_FORMAT_FORMATTER), barField.getActionTime());
		assertEquals(bar.actionTimestamp(), barField.getActionTimestamp());
		assertEquals(bar.closePrice(), barField.getClosePrice());
		assertEquals(bar.highPrice(), barField.getHighPrice());
		assertEquals(bar.lowPrice(), barField.getLowPrice());
		assertEquals(bar.openPrice(), barField.getOpenPrice());
		assertEquals(bar.turnoverDelta(), barField.getTurnoverDelta());
		assertEquals(bar.turnover(), barField.getTurnover());
		assertEquals(bar.volume(), barField.getVolume());
		assertEquals(bar.volumeDelta(), barField.getVolumeDelta());
		assertEquals(bar.openInterest(), barField.getOpenInterest());
		assertEquals(bar.openInterestDelta(), barField.getOpenInterestDelta());
		assertEquals(bar.preSettlePrice(), barField.getPreSettlePrice());
		assertEquals(bar.preOpenInterest(), barField.getPreOpenInterest());
		assertEquals(bar.preSettlePrice(), barField.getPreSettlePrice());
		assertEquals(bar.channelType().toString(), barField.getChannelType());
	}

	@Test
	void testOf() {
		Contract contract = Contract.builder().unifiedSymbol("testContract").build();
		Bar bar = Bar.builder()
				.gatewayId("gatewayId")
				.contract(contract)
				.actionDay(LocalDate.now())
				.actionTime(LocalTime.now().withNano(0))
				.tradingDay(LocalDate.now())
				.actionTimestamp(1L)
				.closePrice(1.0)
				.highPrice(2.0)
				.lowPrice(3.0)
				.openPrice(4.0)
				.turnoverDelta(1.0)
				.turnover(2.0)
				.volume(5)
				.volumeDelta(6)
				.openInterest(7.0)
				.openInterestDelta(8.0)
				.preSettlePrice(9.0)
				.preOpenInterest(10.0)
				.preSettlePrice(11.0)
				.channelType(ChannelType.BIAN)
				.build();
		xyz.redtorch.pb.CoreField.BarField barField = bar.toBarField();
		assertEquals(bar, Bar.of(barField, contract));
	}
}
