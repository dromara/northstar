package org.dromara.northstar.common.model.core;

import static org.junit.jupiter.api.Assertions.*;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.TickType;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreField.TickField;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

class TickTest {

	@Test
	void testToTickField() {
		Tick tick = Tick.builder()
				.gatewayId("testGatewayId")
				.contract(Contract.builder()
						.unifiedSymbol("testSymbol")
						.symbol("testSymbol")
						.exchange(ExchangeEnum.BINANCE)
						.build())
				.actionDay(LocalDate.now())
				.actionTime(LocalTime.now())
				.tradingDay(LocalDate.now())
				.actionTimestamp(System.currentTimeMillis())
				.lastPrice(1.0)
				.avgPrice(2.0)
				.iopv(3.0)
				.volumeDelta(4L)
				.volume(5L)
				.turnover(6.0)
				.turnoverDelta(7.0)
				.openInterest(8.0)
				.openInterestDelta(9.0)
				.settlePrice(10.0)
				.preOpenInterest(11.0)
				.preClosePrice(12.0)
				.preSettlePrice(13.0)
				.openPrice(14.0)
				.highPrice(15.0)
				.lowPrice(16.0)
				.upperLimit(17.0)
				.lowerLimit(18.0)
				.bidPrice(List.of(19.0, 20.0))
				.askPrice(List.of(21.0, 22.0))
				.bidVolume(List.of(23, 24))
				.askVolume(List.of(25, 26))
				.type(TickType.MARKET_TICK)
				.channelType(ChannelType.CTP)
				.build();
		
		TickField tickField = tick.toTickField();
		assertEquals(tick.gatewayId(), tickField.getGatewayId());
		assertEquals(tick.contract().unifiedSymbol(), tickField.getUnifiedSymbol());
		assertEquals(tick.actionDay().format(DateTimeConstant.D_FORMAT_INT_FORMATTER), tickField.getActionDay());
		assertEquals(tick.actionTime().format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER), tickField.getActionTime());
		assertEquals(tick.tradingDay().format(DateTimeConstant.D_FORMAT_INT_FORMATTER), tickField.getTradingDay());
		assertEquals(tick.actionTimestamp(), tickField.getActionTimestamp());
		assertEquals(tick.lastPrice(), tickField.getLastPrice());
		assertEquals(tick.avgPrice(), tickField.getAvgPrice());
		assertEquals(tick.iopv(), tickField.getIopv());
		assertEquals(tick.volumeDelta(), tickField.getVolumeDelta());
		assertEquals(tick.volume(), tickField.getVolume());
		assertEquals(tick.turnover(), tickField.getTurnover());
		assertEquals(tick.turnoverDelta(), tickField.getTurnoverDelta());
		assertEquals(tick.openInterest(), tickField.getOpenInterest());
		assertEquals(tick.openInterestDelta(), tickField.getOpenInterestDelta());
		assertEquals(tick.settlePrice(), tickField.getSettlePrice());
		assertEquals(tick.preOpenInterest(), tickField.getPreOpenInterest());
		assertEquals(tick.preClosePrice(), tickField.getPreClosePrice());
		assertEquals(tick.preSettlePrice(), tickField.getPreSettlePrice());
		assertEquals(tick.openPrice(), tickField.getOpenPrice());
		assertEquals(tick.highPrice(), tickField.getHighPrice());
		assertEquals(tick.lowPrice(), tickField.getLowPrice());
		assertEquals(tick.upperLimit(), tickField.getUpperLimit());
		assertEquals(tick.lowerLimit(), tickField.getLowerLimit());
		assertEquals(tick.bidPrice().get(0), tickField.getBidPriceList().get(0));
		assertEquals(tick.bidPrice().get(1), tickField.getBidPriceList().get(1));
		assertEquals(tick.askPrice().get(0), tickField.getAskPriceList().get(0));
		assertEquals(tick.askPrice().get(1), tickField.getAskPriceList().get(1));
		assertEquals(tick.bidVolume().get(0), tickField.getBidVolumeList().get(0));
		assertEquals(tick.bidVolume().get(1), tickField.getBidVolumeList().get(1));
		assertEquals(tick.askVolume().get(0), tickField.getAskVolumeList().get(0));
		assertEquals(tick.askVolume().get(1), tickField.getAskVolumeList().get(1));
		assertEquals(tick.channelType().toString(), tickField.getChannelType());

	}

}
