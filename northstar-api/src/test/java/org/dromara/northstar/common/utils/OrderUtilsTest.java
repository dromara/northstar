package org.dromara.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.dromara.northstar.common.model.OrderRequest.TradeOperation;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

class OrderUtilsTest {

	@Test
	void testResolveDirection() {
		assertThat(OrderUtils.resolveDirection(TradeOperation.BK)).isEqualTo(DirectionEnum.D_Buy);
		assertThat(OrderUtils.resolveDirection(TradeOperation.BP)).isEqualTo(DirectionEnum.D_Buy);
		assertThat(OrderUtils.resolveDirection(TradeOperation.SK)).isEqualTo(DirectionEnum.D_Sell);
		assertThat(OrderUtils.resolveDirection(TradeOperation.SP)).isEqualTo(DirectionEnum.D_Sell);
	}

	@Test
	void testIsOpenningOrder() {
		assertThat(OrderUtils.isOpenningOrder(TradeOperation.BK)).isTrue();
		assertThat(OrderUtils.isOpenningOrder(TradeOperation.SK)).isTrue();
		assertThat(OrderUtils.isOpenningOrder(TradeOperation.BP)).isFalse();
		assertThat(OrderUtils.isOpenningOrder(TradeOperation.SP)).isFalse();
	}

	@Test
	void testIsClosingOrder() {
		assertThat(OrderUtils.isClosingOrder(TradeOperation.BK)).isFalse();
		assertThat(OrderUtils.isClosingOrder(TradeOperation.SK)).isFalse();
		assertThat(OrderUtils.isClosingOrder(TradeOperation.BP)).isTrue();
		assertThat(OrderUtils.isClosingOrder(TradeOperation.SP)).isTrue();
	}

	@Test
	void testGetClosingDirection() {
		assertThat(OrderUtils.getClosingDirection(DirectionEnum.D_Buy)).isEqualTo(PositionDirectionEnum.PD_Short);
		assertThat(OrderUtils.getClosingDirection(DirectionEnum.D_Sell)).isEqualTo(PositionDirectionEnum.PD_Long);
	}

}
