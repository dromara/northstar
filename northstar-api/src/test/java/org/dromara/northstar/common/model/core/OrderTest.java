package org.dromara.northstar.common.model.core;

import static org.junit.jupiter.api.Assertions.*;

import org.dromara.northstar.common.constant.DateTimeConstant;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;

import java.time.LocalDate;
import java.time.LocalTime;

class OrderTest {

	@Test
	void testToOrderField() {
		Order order = Order.builder()
				.gatewayId("gatewayId")
				.originOrderId("originOrderId")
				.orderId("orderId")
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.hedgeFlag(HedgeFlagEnum.HF_Speculation)
				.orderPriceType(OrderPriceTypeEnum.OPT_LimitPrice)
				.orderStatus(OrderStatusEnum.OS_AllTraded)
				.price(1.0)
				.totalVolume(1)
				.tradedVolume(1)
				.timeCondition(TimeConditionEnum.TC_GFA)
				.gtdDate("gtdDate")
				.volumeCondition(VolumeConditionEnum.VC_AV)
				.minVolume(1)
				.contingentCondition(ContingentConditionEnum.CC_Immediately)
				.stopPrice(1.0)
				.forceCloseReason(ForceCloseReasonEnum.FCR_ClientOverPositionLimit)
				.tradingDay(LocalDate.now())
				.orderDate(LocalDate.now())
				.orderTime(LocalTime.now())
				.updateDate(LocalDate.now())
				.updateTime(LocalTime.now())
				.statusMsg("statusMsg")
				.build();
		xyz.redtorch.pb.CoreField.OrderField orderField = order.toOrderField();
		assertEquals(order.gatewayId(), orderField.getGatewayId());
		assertEquals(order.originOrderId(), orderField.getOriginOrderId());
		assertEquals(order.orderId(), orderField.getOrderId());
		assertEquals(order.direction().getNumber(), orderField.getDirection().getNumber());
		assertEquals(order.offsetFlag().getNumber(), orderField.getOffsetFlag().getNumber());
		assertEquals(order.hedgeFlag().getNumber(), orderField.getHedgeFlag().getNumber());
		assertEquals(order.orderPriceType().getNumber(), orderField.getOrderPriceType().getNumber());
		assertEquals(order.orderStatus().getNumber(), orderField.getOrderStatus().getNumber());
		assertEquals(order.price(), orderField.getPrice());
		assertEquals(order.totalVolume(), orderField.getTotalVolume());
		assertEquals(order.tradedVolume(), orderField.getTradedVolume());
		assertEquals(order.timeCondition().getNumber(), orderField.getTimeCondition().getNumber());
		assertEquals(order.gtdDate(), orderField.getGtdDate());
		assertEquals(order.volumeCondition().getNumber(), orderField.getVolumeCondition().getNumber());
		assertEquals(order.minVolume(), orderField.getMinVolume());
		assertEquals(order.contingentCondition().getNumber(), orderField.getContingentCondition().getNumber());
		assertEquals(order.stopPrice(), orderField.getStopPrice());
		assertEquals(order.forceCloseReason().getNumber(), orderField.getForceCloseReason().getNumber());
		assertEquals(order.tradingDay().format(DateTimeConstant.D_FORMAT_INT_FORMATTER), orderField.getTradingDay());
		assertEquals(order.orderDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER), orderField.getOrderDate());
		assertEquals(order.orderTime().format(DateTimeConstant.T_FORMAT_FORMATTER), orderField.getOrderTime());
		assertEquals(order.updateTime().format(DateTimeConstant.T_FORMAT_FORMATTER), orderField.getUpdateTime());

	}

}
