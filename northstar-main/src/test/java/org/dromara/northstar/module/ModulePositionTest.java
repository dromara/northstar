package org.dromara.northstar.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.function.BiConsumer;

import org.dromara.northstar.common.constant.ClosingPolicy;
import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

class ModulePositionTest {

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
	
	@SuppressWarnings("unchecked")
	BiConsumer<TradeField, TradeField> onDealCallback = mock(BiConsumer.class);
	
	// 用例：行情更新，持仓利润更新
	@Test
	void shouldUpdateProfit() {
		ModulePosition mp = new ModulePosition("testGateway", openTrade.getContract(), openTrade.getDirection(), ClosingPolicy.FIRST_IN_LAST_OUT, onDealCallback, List.of(openTrade));
		mp.onTick(tick1);
		assertThat(mp.profit()).isCloseTo(2220D, offset(1e-6));
	}
	
	// 用例：忽略非相关行情
	@Test
	void shouldNotUpdateProfit() {
		ModulePosition mp = new ModulePosition("testGateway", openTrade.getContract(), openTrade.getDirection(), ClosingPolicy.FIRST_IN_LAST_OUT, onDealCallback, List.of(openTrade));
		mp.onTick(tick2);
		assertThat(mp.profit()).isCloseTo(0D, offset(1e-6));
	}
	
	// 用例：加仓
	@Test
	void shouldAddPosition() {
		ModulePosition mp = new ModulePosition("testGateway", openTrade.getContract(), openTrade.getDirection(), ClosingPolicy.FIRST_IN_LAST_OUT, onDealCallback, List.of(openTrade));
		mp.onTrade(openTrade1);
		mp.onTrade(openTrade2);
		mp.onTick(tick1);
		assertThat(mp.tdAvailable()).isEqualTo(1);
		assertThat(mp.ydAvailable()).isEqualTo(2);
		assertThat(mp.totalVolume()).isEqualTo(3);
		assertThat(mp.avgOpenPrice()).isCloseTo(5100, offset(1e-6));
		assertThat(mp.profit()).isCloseTo(330D, offset(1e-6));
	}
	
	// 用例：平仓
	@Test
	void shouldClosePosition() {
		ModulePosition mp = new ModulePosition("testGateway", openTrade.getContract(), openTrade.getDirection(), ClosingPolicy.FIRST_IN_LAST_OUT, onDealCallback, List.of(openTrade));
		mp.onTrade(closeTrade);
		assertThat(mp.tdAvailable()).isZero();
		assertThat(mp.ydAvailable()).isZero();
		assertThat(mp.totalVolume()).isZero();
	}
		
	// 用例：减仓，平今优先
	@Test
	void shouldReduceTdPosition() {
		ModulePosition mp = new ModulePosition("testGateway", openTrade.getContract(), openTrade.getDirection(), ClosingPolicy.FIRST_IN_LAST_OUT, onDealCallback, List.of(openTrade));
		mp.onTrade(openTrade1);
		mp.onTick(tick1);
		mp.onTrade(closeTrade);
		assertThat(mp.tdAvailable()).isEqualTo(0);
		assertThat(mp.ydAvailable()).isEqualTo(1);
		assertThat(mp.totalVolume()).isEqualTo(1);
		assertThat(mp.avgOpenPrice()).isCloseTo(5000, offset(1e-6));
		assertThat(mp.profit()).isCloseTo(1110D, offset(1e-6));
	}
	
	// 用例：减仓，先开先平
	@Test
	void shouldReduceYdPosition() {
		ModulePosition mp = new ModulePosition("testGateway", openTrade.getContract(), openTrade.getDirection(), ClosingPolicy.FIRST_IN_FIRST_OUT, onDealCallback, List.of(openTrade));
		mp.onTrade(openTrade1);
		mp.onTick(tick1);
		mp.onTrade(closeTrade);
		assertThat(mp.tdAvailable()).isEqualTo(1);
		assertThat(mp.ydAvailable()).isEqualTo(0);
		assertThat(mp.totalVolume()).isEqualTo(1);
		assertThat(mp.avgOpenPrice()).isCloseTo(5300, offset(1e-6));
		assertThat(mp.profit()).isCloseTo(-1890D, offset(1e-6));
	}
	
	// 用例：平仓委托，冻结持仓；撤销委托，解冻持仓
	@Test
	void shouldHandlerOrder() {
		ModulePosition mp = new ModulePosition("testGateway", openTrade.getContract(), openTrade.getDirection(), ClosingPolicy.FIRST_IN_LAST_OUT, onDealCallback, List.of(openTrade));
		mp.onTick(tick1);
		mp.onTrade(openTrade1);
		mp.onOrder(order1);
		mp.onTick(tick1);
		assertThat(mp.totalVolume()).isEqualTo(3);
		assertThat(mp.totalAvailable()).isEqualTo(2);
		assertThat(mp.tdVolume()).isEqualTo(1);
		assertThat(mp.tdAvailable()).isEqualTo(0);
		assertThat(mp.ydVolume()).isEqualTo(2);
		assertThat(mp.ydAvailable()).isEqualTo(2);
	}
	
	// 用例：平仓委托，冻结持仓；撤销委托，解冻持仓
	@Test
	void shouldHandlerOrder2() {
		ModulePosition mp = new ModulePosition("testGateway", openTrade.getContract(), openTrade.getDirection(), ClosingPolicy.FIRST_IN_LAST_OUT, onDealCallback, List.of(openTrade));
		mp.onTick(tick1);
		mp.onTrade(openTrade1);
		mp.onOrder(order2);
		mp.onTick(tick1);
		assertThat(mp.totalVolume()).isEqualTo(3);
		assertThat(mp.totalAvailable()).isEqualTo(2);
		assertThat(mp.tdVolume()).isEqualTo(1);
		assertThat(mp.tdAvailable()).isEqualTo(1);
		assertThat(mp.ydVolume()).isEqualTo(2);
		assertThat(mp.ydAvailable()).isEqualTo(1);
	}
	
	// 用例：占用保证金
	@Test
	void shouldTakeMargin() {
		ModulePosition mp = new ModulePosition("testGateway", openTrade.getContract(), openTrade.getDirection(), ClosingPolicy.FIRST_IN_LAST_OUT, onDealCallback, List.of(openTrade));
		mp.onTrade(openTrade1);
		assertThat(mp.totalMargin()).isCloseTo(12240, offset(1e-6));
	}

}
