package org.dromara.northstar.common.model.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;

class SubmitOrderReqTest {

	@Test
	void testToString() {
		SubmitOrderReq orderReq = SubmitOrderReq.builder()
				.gatewayId("testGatewayId")
				.contract(Contract.builder()
						.unifiedSymbol("testSymbol")
						.symbol("testSymbol")
						.build())
				.originOrderId("testTradeId")
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(1.0)
				.volume(2)
				.orderPriceType(OrderPriceTypeEnum.OPT_AnyPrice)
				.timeCondition(TimeConditionEnum.TC_GFD)
				.volumeCondition(VolumeConditionEnum.VC_AV)
				.minVolume(1)
				.contingentCondition(ContingentConditionEnum.CC_Immediately)
				.stopPrice(1.0)
				.build();
		assertDoesNotThrow(() -> {
			System.out.println(orderReq);
		});
	}

}
