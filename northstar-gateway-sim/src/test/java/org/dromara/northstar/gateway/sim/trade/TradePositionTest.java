package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.LocalDate;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

class TradePositionTest {
	
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

	// 用例：初始化检验
	@Test
	void shouldInitSuccessfully() {
		assertDoesNotThrow(() -> {
			new TradePosition(openTrade.contract(), DirectionEnum.D_Buy);
		});
	}
	
	// 用例：行情更新，持仓利润更新
	@Test
	void shouldUpdateProfit() {
		TradePosition tp = new TradePosition(openTrade.contract(), DirectionEnum.D_Buy);
		tp.onTrade(openTrade);
		tp.onTick(tick1);
		assertThat(tp.profit()).isCloseTo(2220D, offset(1e-6));
	}
	
	// 用例：忽略非相关行情
	@Test
	void shouldNotUpdateProfit() {
		TradePosition tp = new TradePosition(openTrade.contract(), DirectionEnum.D_Buy);
		tp.onTrade(openTrade);
		tp.onTick(tick2);
		assertThat(tp.profit()).isCloseTo(0D, offset(1e-6));
	}
	
	// 用例：加仓
	@Test
	void shouldAddPosition() {
		TradePosition tp = new TradePosition(openTrade.contract(), DirectionEnum.D_Buy);
		tp.onTrade(openTrade);
		tp.onTrade(openTrade1);
		tp.onTick(tick1);
		assertThat(tp.tdAvailable()).isEqualTo(1);
		assertThat(tp.ydAvailable()).isEqualTo(2);
		assertThat(tp.totalVolume()).isEqualTo(3);
		assertThat(tp.avgOpenPrice()).isCloseTo(5100, offset(1e-6));
		assertThat(tp.profit()).isCloseTo(330D, offset(1e-6));
	}
	
	// 用例：平仓
	@Test
	void shouldClosePosition() {
		TradePosition tp = new TradePosition(openTrade.contract(), DirectionEnum.D_Buy);
		tp.onTrade(openTrade);
		tp.onTrade(closeTrade);
		assertThat(tp.tdAvailable()).isZero();
		assertThat(tp.ydAvailable()).isZero();
		assertThat(tp.totalVolume()).isZero();
	}
		
	// 用例：减仓，先开先平
	@Test
	void shouldReduceYdPosition() {
		TradePosition tp = new TradePosition(openTrade.contract(), DirectionEnum.D_Buy);
		tp.onTrade(openTrade);
		tp.onTrade(openTrade1);
		tp.onTick(tick1);
		assertThat(tp.onTrade(closeTrade).stream().mapToDouble(Deal::profit).sum()).isCloseTo(4000D, offset(1e-6));
		assertThat(tp.tdAvailable()).isEqualTo(1);
		assertThat(tp.ydAvailable()).isEqualTo(0);
		assertThat(tp.totalVolume()).isEqualTo(1);
		assertThat(tp.avgOpenPrice()).isCloseTo(5300, offset(1e-6));
		assertThat(tp.profit()).isCloseTo(-1890D, offset(1e-6));
	}
	
	// 用例：平仓委托，冻结持仓；撤销委托，解冻持仓
	@Test
	void shouldHandlerOrder() {
		TradePosition tp = new TradePosition(openTrade.contract(), DirectionEnum.D_Buy);
		tp.onTrade(openTrade);
		tp.onTrade(openTrade1);
		tp.onOrder(order1);
		tp.onTick(tick1);
		assertThat(tp.totalVolume()).isEqualTo(3);
		assertThat(tp.totalAvailable()).isEqualTo(2);
		assertThat(tp.tdVolume()).isEqualTo(1);
		assertThat(tp.tdAvailable()).isEqualTo(0);
		assertThat(tp.ydVolume()).isEqualTo(2);
		assertThat(tp.ydAvailable()).isEqualTo(2);
	}
	
	// 用例：平仓委托，冻结持仓；撤销委托，解冻持仓
	@Test
	void shouldHandlerOrder2() {
		TradePosition tp = new TradePosition(openTrade.contract(), DirectionEnum.D_Buy);
		tp.onTrade(openTrade);
		tp.onTrade(openTrade1);
		tp.onOrder(order2);
		tp.onTick(tick1);
		assertThat(tp.totalVolume()).isEqualTo(3);
		assertThat(tp.totalAvailable()).isEqualTo(2);
		assertThat(tp.tdVolume()).isEqualTo(1);
		assertThat(tp.tdAvailable()).isEqualTo(1);
		assertThat(tp.ydVolume()).isEqualTo(2);
		assertThat(tp.ydAvailable()).isEqualTo(1);
	}
	
	// 用例：占用保证金
	@Test
	void shouldTakeMargin() {
		TradePosition tp = new TradePosition(openTrade.contract(), DirectionEnum.D_Buy);
		tp.onTrade(openTrade);
		tp.onTrade(openTrade1);
		assertThat(tp.totalMargin()).isCloseTo(12240, offset(1e-6));
	}
}
