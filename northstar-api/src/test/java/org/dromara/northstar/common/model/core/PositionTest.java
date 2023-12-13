package org.dromara.northstar.common.model.core;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField;

class PositionTest {

	@Test
	void testToPositionField() {
		Position position = Position.builder()
				.positionId("positionId")
				.positionDirection(PositionDirectionEnum.PD_Long)
				.position(1)
				.frozen(2)
				.ydPosition(3)
				.ydFrozen(4)
				.tdPosition(5)
				.tdFrozen(6)
				.openPrice(11)
				.openPriceDiff(12)
				.positionProfit(13)
				.positionProfitRatio(14)
				.useMargin(15)
				.exchangeMargin(16)
				.contractValue(17)
				.gatewayId("gatewayId")
				.build();
		CoreField.PositionField positionField = position.toPositionField();
		assertThat(positionField.getPositionId()).isEqualTo(position.positionId());
		assertThat(positionField.getPositionDirection()).isEqualTo(position.positionDirection());
		assertThat(positionField.getPosition()).isEqualTo(position.position());
		assertThat(positionField.getFrozen()).isEqualTo(position.frozen());
		assertThat(positionField.getYdPosition()).isEqualTo(position.ydPosition());
		assertThat(positionField.getYdFrozen()).isEqualTo(position.ydFrozen());
		assertThat(positionField.getTdPosition()).isEqualTo(position.tdPosition());
		assertThat(positionField.getTdFrozen()).isEqualTo(position.tdFrozen());
		assertThat(positionField.getOpenPrice()).isEqualTo(position.openPrice());
		assertThat(positionField.getOpenPriceDiff()).isEqualTo(position.openPriceDiff());
		assertThat(positionField.getPositionProfit()).isEqualTo(position.positionProfit());
		assertThat(positionField.getPositionProfitRatio()).isEqualTo(position.positionProfitRatio());
		assertThat(positionField.getUseMargin()).isEqualTo(position.useMargin());
		assertThat(positionField.getExchangeMargin()).isEqualTo(position.exchangeMargin());
		assertThat(positionField.getContractValue()).isEqualTo(position.contractValue());
		assertThat(positionField.getGatewayId()).isEqualTo(position.gatewayId());

	}

}
