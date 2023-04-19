package org.dromara.northstar.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.module.legacy.TradePosition;
import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

class TradePositionTest {

	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	TradeField openTrade = factory.makeTradeField("rb2205@SHFE", 5000, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, "20220404");
	TradeField openTrade1 = factory.makeTradeField("rb2205@SHFE", 5300, 1, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
	TradeField openTrade2 = factory.makeTradeField("rb2210@SHFE", 5000, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
	TradeField openTrade10 = factory.makeTradeField("rb2205@SHFE", 5300, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open);
	TradeField openTrade11 = factory.makeTradeField("rb2205@SHFE", 5000, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open);
	TradeField closeTrade = factory.makeTradeField("rb2205@SHFE", 5200, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close);

	TickField tick1 = factory.makeTickField("rb2205@SHFE", 5111);
	TickField tick2 = factory.makeTickField("rb2210@SHFE", 5111);
	
	OrderField order1 = factory.makeOrderField("rb2205@SHFE", 5300, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday);
	OrderField order2 = factory.makeOrderField("rb2205@SHFE", 5300, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseYesterday);
	// 用例：初始化检验
	@Test
	void shouldInitSuccessfully() {
		assertDoesNotThrow(() -> {
			new TradePosition(List.of(openTrade), ClosingPolicy.PRIOR_TODAY);
		});
	}
	
	@Test
	void shouldFailInit() {
		assertThrows(IllegalArgumentException.class, ()->{
			new TradePosition(List.of(), ClosingPolicy.PRIOR_TODAY);
		});
		
		assertThrows(IllegalArgumentException.class, ()->{
			new TradePosition(List.of(openTrade, openTrade2), ClosingPolicy.PRIOR_TODAY);
		});
	}
	
	// 用例：行情更新，持仓利润更新
	@Test
	void shouldUpdateProfit() {
		TradePosition tp = new TradePosition(List.of(openTrade), ClosingPolicy.PRIOR_TODAY);
		tp.updateTick(tick1);
		assertThat(tp.profit()).isCloseTo(2220D, offset(1e-6));
	}
	
	// 用例：忽略非相关行情
	@Test
	void shouldNotUpdateProfit() {
		TradePosition tp = new TradePosition(List.of(openTrade), ClosingPolicy.PRIOR_TODAY);
		tp.updateTick(tick2);
		assertThat(tp.profit()).isCloseTo(0D, offset(1e-6));
	}
	
	// 用例：加仓
	@Test
	void shouldAddPosition() {
		TradePosition tp = new TradePosition(List.of(openTrade), ClosingPolicy.PRIOR_TODAY);
		tp.onTrade(openTrade1);
		tp.onTrade(openTrade2);
		tp.updateTick(tick1);
		assertThat(tp.tdAvailable()).isEqualTo(1);
		assertThat(tp.ydAvailable()).isEqualTo(2);
		assertThat(tp.totalVolume()).isEqualTo(3);
		assertThat(tp.avgOpenPrice()).isCloseTo(5100, offset(1e-6));
		assertThat(tp.profit()).isCloseTo(330D, offset(1e-6));
	}
	
	// 用例：平仓
	@Test
	void shouldClosePosition() {
		TradePosition tp = new TradePosition(List.of(openTrade), ClosingPolicy.PRIOR_TODAY);
		tp.onTrade(closeTrade);
		assertThat(tp.tdAvailable()).isZero();
		assertThat(tp.ydAvailable()).isZero();
		assertThat(tp.totalVolume()).isZero();
	}
		
	// 用例：减仓，平今优先
	@Test
	void shouldReduceTdPosition() {
		TradePosition tp = new TradePosition(List.of(openTrade), ClosingPolicy.PRIOR_TODAY);
		tp.onTrade(openTrade1);
		tp.updateTick(tick1);
		assertThat(tp.onTrade(closeTrade)).isCloseTo(1000D, offset(1e-6));
		assertThat(tp.tdAvailable()).isEqualTo(0);
		assertThat(tp.ydAvailable()).isEqualTo(1);
		assertThat(tp.totalVolume()).isEqualTo(1);
		assertThat(tp.avgOpenPrice()).isCloseTo(5000, offset(1e-6));
		assertThat(tp.profit()).isCloseTo(1110D, offset(1e-6));
	}
	
	// 用例：减仓，先开先平
	@Test
	void shouldReduceYdPosition() {
		TradePosition tp = new TradePosition(List.of(openTrade), ClosingPolicy.FIFO);
		tp.onTrade(openTrade1);
		tp.updateTick(tick1);
		assertThat(tp.onTrade(closeTrade)).isCloseTo(4000D, offset(1e-6));
		assertThat(tp.tdAvailable()).isEqualTo(1);
		assertThat(tp.ydAvailable()).isEqualTo(0);
		assertThat(tp.totalVolume()).isEqualTo(1);
		assertThat(tp.avgOpenPrice()).isCloseTo(5300, offset(1e-6));
		assertThat(tp.profit()).isCloseTo(-1890D, offset(1e-6));
	}
	
	// 用例：平仓委托，冻结持仓；撤销委托，解冻持仓
	@Test
	void shouldHandlerOrder() {
		TradePosition tp = new TradePosition(List.of(openTrade), ClosingPolicy.FIFO);
		tp.onTrade(openTrade1);
		tp.onOrder(order1);
		tp.updateTick(tick1);
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
		TradePosition tp = new TradePosition(List.of(openTrade), ClosingPolicy.FIFO);
		tp.onTrade(openTrade1);
		tp.onOrder(order2);
		tp.updateTick(tick1);
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
		TradePosition tp = new TradePosition(List.of(openTrade), ClosingPolicy.FIFO);
		tp.onTrade(openTrade1);
		assertThat(tp.totalMargin()).isCloseTo(12240, offset(1e-6));
	}
}
