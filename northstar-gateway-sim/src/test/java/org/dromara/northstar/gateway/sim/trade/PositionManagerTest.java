package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

class PositionManagerTest {
	
	SimGatewayAccount account = new SimGatewayAccount("testAccount");
	PositionManager posMgr = new PositionManager(account);

	TestFieldFactory factory = new TestFieldFactory("testGateway");
	TradeField openTrade = factory.makeTradeField("rb2205", 5000, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, "20220404");
	TradeField openTrade1 = factory.makeTradeField("rb2205", 5300, 1, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
	TradeField openTrade2 = factory.makeTradeField("rb2210", 5000, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
	TradeField openTrade10 = factory.makeTradeField("rb2205", 5300, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open);
	TradeField openTrade11 = factory.makeTradeField("rb2205", 5000, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open);
	TradeField closeTrade = factory.makeTradeField("rb2205", 5200, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close);

	TickField tick1 = factory.makeTickField("rb2205", 5111);
	TickField tick2 = factory.makeTickField("rb2210", 5111);
	
	OrderField order1 = factory.makeOrderField("rb2205", 5300, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday);
	OrderField order2 = factory.makeOrderField("rb2205", 5300, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseYesterday);
	
	@Test
	void testOnOrder() {
		posMgr.onTrade(openTrade);
		posMgr.onOrder(order1);
		
		assertThat(posMgr.getAvailablePosition(DirectionEnum.D_Buy, openTrade.getContract().getUnifiedSymbol(), false)).isEqualTo(1);
	}

	@Test
	void testOnTrade() {
		posMgr.onTrade(openTrade);
		
		assertThat(posMgr.getAvailablePosition(DirectionEnum.D_Buy, openTrade.getContract().getUnifiedSymbol(), false)).isEqualTo(2);
		assertThat(posMgr.totalMargin()).isCloseTo(8000, offset(1D));
	}

	@Test
	void testPositionFields() {
		assertThat(posMgr.positionFields()).isEmpty();
		
		posMgr.onTrade(openTrade);
		assertThat(posMgr.positionFields()).hasSize(1);
		posMgr.onTrade(openTrade1);
		assertThat(posMgr.positionFields()).hasSize(1);
	}

	@Test
	void testGetNonclosedTrade() {
		posMgr.onTrade(openTrade);
		posMgr.onTrade(openTrade1);
		assertThat(posMgr.getNonclosedTrade()).hasSize(2);
	}

	@Test
	void testTotalHoldingProfit() {
		posMgr.onTrade(openTrade);
		posMgr.onTrade(openTrade1);
		posMgr.onTick(tick1);
		assertThat(posMgr.totalHoldingProfit()).isCloseTo(330, offset(1D));
	}

}
