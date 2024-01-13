package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.api.Assertions.*;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Trade;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

class DealTest {

	Contract contract = Contract.builder().unifiedSymbol("rb2205@SHFE@FUTURES").multiplier(10).exchange(CoreEnum.ExchangeEnum.SHFE).build();
	Contract contract2 = Contract.builder().unifiedSymbol("rb2210@SHFE@FUTURES").multiplier(10).exchange(CoreEnum.ExchangeEnum.SHFE).build();
	
	Trade openTrade = Trade.builder().contract(contract).price(5000).volume(2).direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open).build();
	Trade openTrade1 = Trade.builder().contract(contract).price(5300).volume(1).direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open).build();
	Trade closeTrade = Trade.builder().contract(contract).price(5200).volume(2).direction(DirectionEnum.D_Sell)
			.offsetFlag(OffsetFlagEnum.OF_Close).build();
	Trade closeTrade1 = Trade.builder().contract(contract2).price(5200).volume(2).direction(DirectionEnum.D_Sell)
			.offsetFlag(OffsetFlagEnum.OF_Close).build();

	@Test
	void testNotMatch() {
		assertThrows(IllegalArgumentException.class, () -> {
			Deal.builder().openTrade(openTrade).closeTrade(openTrade1).build();
		});
	}
	
	@Test
	void testNotMatch2() {
		assertThrows(IllegalArgumentException.class, () -> {
			Deal.builder().openTrade(openTrade).closeTrade(closeTrade1).build();
		});
	}

	@Test
	void testProfit() {
		assertThat(Deal.builder().openTrade(openTrade).closeTrade(closeTrade).build().profit()).isCloseTo(4000,
				offset(1e-4));
	}

}
