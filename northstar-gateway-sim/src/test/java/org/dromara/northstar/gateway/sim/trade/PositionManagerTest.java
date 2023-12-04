package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.time.LocalDate;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

class PositionManagerTest {
	
	SimGatewayAccount account = new SimGatewayAccount("testAccount");
	PositionManager posMgr = new PositionManager(account);
	
	LocalDate today = LocalDate.now();
	Contract c1 = Contract.builder().unifiedSymbol("rb2205@SHFE").multiplier(10).longMarginRatio(0.08).shortMarginRatio(0.08).build();
	Contract c2 = Contract.builder().unifiedSymbol("rb2210@SHFE").multiplier(10).longMarginRatio(0.08).shortMarginRatio(0.08).build();
	
	Trade openTrade = Trade.builder().tradingDay(today.minusDays(1)).contract(c1).price(5000).volume(2).direction(DirectionEnum.D_Buy).offsetFlag(OffsetFlagEnum.OF_Open).build();
	Trade openTrade1 = Trade.builder().tradingDay(today).contract(c1).price(5300).volume(1).direction(DirectionEnum.D_Buy).offsetFlag(OffsetFlagEnum.OF_Open).build();
	Trade closeTrade = Trade.builder().tradingDay(today).contract(c1).price(5200).volume(2).direction(DirectionEnum.D_Sell).offsetFlag(OffsetFlagEnum.OF_Close).build();
	Tick tick1 = Tick.builder().tradingDay(today).contract(c1).lastPrice(5111).build();
	Tick tick2 = Tick.builder().tradingDay(today).contract(c2).lastPrice(5111).build();
	
	Order order1 = Order.builder().tradingDay(today).contract(c1).price(5300).totalVolume(1).direction(DirectionEnum.D_Sell).offsetFlag(OffsetFlagEnum.OF_CloseToday).build();
	Order order2 = Order.builder().tradingDay(today).contract(c1).price(5300).totalVolume(1).direction(DirectionEnum.D_Sell).offsetFlag(OffsetFlagEnum.OF_CloseYesterday).build();

	@Test
	void testOnOrder() {
		posMgr.onTrade(openTrade);
		posMgr.onOrder(order1);
		
		assertThat(posMgr.getAvailablePosition(DirectionEnum.D_Buy, openTrade.contract())).isEqualTo(1);
	}

	@Test
	void testOnTrade() {
		posMgr.onTrade(openTrade);
		
		assertThat(posMgr.getAvailablePosition(DirectionEnum.D_Buy, openTrade.contract())).isEqualTo(2);
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
