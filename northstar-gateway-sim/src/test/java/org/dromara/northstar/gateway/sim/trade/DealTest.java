package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.TradeField;

class DealTest {
	
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	TradeField openTrade = factory.makeTradeField("rb2205@SHFE", 5000, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, "20220404");
	TradeField openTrade1 = factory.makeTradeField("rb2205@SHFE", 5300, 1, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
	TradeField openTrade2 = factory.makeTradeField("rb2210@SHFE", 5000, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
	TradeField openTrade10 = factory.makeTradeField("rb2205@SHFE", 5300, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open);
	TradeField openTrade11 = factory.makeTradeField("rb2205@SHFE", 5000, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open);
	TradeField closeTrade = factory.makeTradeField("rb2205@SHFE", 5200, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close);
	TradeField closeTrade1 = factory.makeTradeField("rb2210@SHFE", 5200, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close);

	@Test
	void testNotMatch() {
		assertThrows(IllegalArgumentException.class, () -> {
			Deal.builder().openTrade(openTrade).closeTrade(openTrade1).build();
		});
		
		assertThrows(IllegalArgumentException.class, () -> {
			Deal.builder().openTrade(openTrade).closeTrade(closeTrade1).build();
		});
	}
	
	@Test
	void testProfit() {
		assertThat(Deal.builder().openTrade(openTrade).closeTrade(closeTrade).build().profit()).isCloseTo(4000, offset(1e-4));
	}

}
