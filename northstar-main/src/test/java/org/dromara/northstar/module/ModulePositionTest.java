package org.dromara.northstar.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiConsumer;

import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

class ModulePositionTest {

	ContractDefinition cd = ContractDefinition.builder().commissionFee(0).build();
	Contract contract = Contract.builder().unifiedSymbol("rb2205@SHFE@FUTURES").contractDefinition(cd).multiplier(10).longMarginRatio(0.08).shortMarginRatio(0.08).build();
	Contract contract2 = Contract.builder().unifiedSymbol("rb2210@SHFE@FUTURES").contractDefinition(cd).multiplier(10).longMarginRatio(0.08).shortMarginRatio(0.08).build();
	LocalDate today = LocalDate.now();
	Tick tick1 = Tick.builder().tradingDay(today).contract(contract).lastPrice(5111).build();
	Tick tick2 = Tick.builder().tradingDay(today).contract(contract2).lastPrice(5111).build();
	Trade openTrade = Trade.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.tradingDay(LocalDate.of(2022, 4, 4))
			.price(5000)
			.volume(2)
			.build();
	Trade openTrade1 = Trade.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.tradingDay(LocalDate.now())
			.price(5300)
			.volume(1)
			.build();
	Trade openTrade2 = Trade.builder()
			.contract(contract2)
			.direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.tradingDay(LocalDate.now())
			.price(5000)
			.volume(2)
			.build();
	Trade closeTrade = Trade.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Sell)
			.offsetFlag(OffsetFlagEnum.OF_Close)
			.tradingDay(LocalDate.now())
			.price(5200)
			.volume(2)
			.build();
	Order order1 = Order.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Sell)
			.offsetFlag(OffsetFlagEnum.OF_CloseToday)
			.price(5300)
			.totalVolume(1)
			.build();
	Order order2 = Order.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Sell)
			.offsetFlag(OffsetFlagEnum.OF_CloseYesterday)
			.price(5300)
			.totalVolume(1)
			.build();

	@SuppressWarnings("unchecked")
	BiConsumer<Trade, Trade> onDealCallback = mock(BiConsumer.class);

	// 用例：行情更新，持仓利润更新
	@Test
	void shouldUpdateProfit() {
		ModulePosition mp = new ModulePosition(openTrade.contract(), openTrade.direction(), ClosingPolicy.FIRST_IN_LAST_OUT, onDealCallback, List.of(openTrade));
		mp.onTick(tick1);
		assertThat(mp.profit()).isCloseTo(2220D, offset(1e-6));
	}

	// 用例：忽略非相关行情
	@Test
	void shouldNotUpdateProfit() {
		ModulePosition mp = new ModulePosition(openTrade.contract(), openTrade.direction(), ClosingPolicy.FIRST_IN_LAST_OUT, onDealCallback, List.of(openTrade));
		mp.onTick(tick2);
		assertThat(mp.profit()).isCloseTo(0D, offset(1e-6));
	}

	// 用例：加仓
	@Test
	void shouldAddPosition() {
		ModulePosition mp = new ModulePosition(openTrade.contract(), openTrade.direction(), ClosingPolicy.FIRST_IN_LAST_OUT, onDealCallback, List.of(openTrade));
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
		ModulePosition mp = new ModulePosition(openTrade.contract(), openTrade.direction(), ClosingPolicy.FIRST_IN_LAST_OUT, onDealCallback, List.of(openTrade));
		mp.onTrade(closeTrade);
		assertThat(mp.tdAvailable()).isZero();
		assertThat(mp.ydAvailable()).isZero();
		assertThat(mp.totalVolume()).isZero();
	}

	// 用例：减仓，平今优先
	@Test
	void shouldReduceTdPosition() {
		ModulePosition mp = new ModulePosition(openTrade.contract(), openTrade.direction(), ClosingPolicy.FIRST_IN_LAST_OUT, onDealCallback, List.of(openTrade));
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
		ModulePosition mp = new ModulePosition(openTrade.contract(), openTrade.direction(), ClosingPolicy.FIRST_IN_FIRST_OUT, onDealCallback, List.of(openTrade));
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
		ModulePosition mp = new ModulePosition(openTrade.contract(), openTrade.direction(), ClosingPolicy.FIRST_IN_LAST_OUT, onDealCallback, List.of(openTrade));
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
		ModulePosition mp = new ModulePosition(openTrade.contract(), openTrade.direction(), ClosingPolicy.FIRST_IN_LAST_OUT, onDealCallback, List.of(openTrade));
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
		ModulePosition mp = new ModulePosition(openTrade.contract(), openTrade.direction(), ClosingPolicy.FIRST_IN_LAST_OUT, onDealCallback, List.of(openTrade));
		mp.onTrade(openTrade1);
		assertThat(mp.totalMargin()).isCloseTo(12240, offset(1e-6));
	}

}
