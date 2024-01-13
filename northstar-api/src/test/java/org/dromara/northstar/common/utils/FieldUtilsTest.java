package org.dromara.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

public class FieldUtilsTest {

	@Test
	public void testChnDirectionEnum() {
		assertThat(FieldUtils.chn(DirectionEnum.D_Buy)).isEqualTo("多");
		assertThat(FieldUtils.chn(DirectionEnum.D_Sell)).isEqualTo("空");
		assertThat(FieldUtils.chn(DirectionEnum.D_Unknown)).isEqualTo("未知");
	}

	@Test
	public void testChnOffsetFlagEnum() {
		assertThat(FieldUtils.chn(OffsetFlagEnum.OF_Open)).isEqualTo("开");
		assertThat(FieldUtils.chn(OffsetFlagEnum.OF_Close)).isEqualTo("平");
		assertThat(FieldUtils.chn(OffsetFlagEnum.OF_CloseToday)).isEqualTo("平今");
		assertThat(FieldUtils.chn(OffsetFlagEnum.OF_CloseYesterday)).isEqualTo("平昨");
		assertThat(FieldUtils.chn(OffsetFlagEnum.OF_Unknown)).isEqualTo("未知");
		assertThat(FieldUtils.chn(OffsetFlagEnum.OF_ForceClose)).isEqualTo("强平");
		assertThat(FieldUtils.chn(OffsetFlagEnum.OF_ForceOff)).isEqualTo("强平");
		assertThat(FieldUtils.chn(OffsetFlagEnum.OF_LocalForceClose)).isEqualTo("强平");
	}

	@Test
	public void testChnOrderStatusEnum() {
		assertThat(FieldUtils.chn(OrderStatusEnum.OS_AllTraded)).isEqualTo("全成");
		assertThat(FieldUtils.chn(OrderStatusEnum.OS_Canceled)).isEqualTo("已撤单");
		assertThat(FieldUtils.chn(OrderStatusEnum.OS_NoTradeNotQueueing)).isEqualTo("未排队");
		assertThat(FieldUtils.chn(OrderStatusEnum.OS_NoTradeQueueing)).isEqualTo("已挂单");
		assertThat(FieldUtils.chn(OrderStatusEnum.OS_NotTouched)).isEqualTo("未知");
		assertThat(FieldUtils.chn(OrderStatusEnum.OS_PartTradedNotQueueing)).isEqualTo("部分未排队");
		assertThat(FieldUtils.chn(OrderStatusEnum.OS_PartTradedQueueing)).isEqualTo("部分成交");
		assertThat(FieldUtils.chn(OrderStatusEnum.OS_Rejected)).isEqualTo("已拒绝");
		assertThat(FieldUtils.chn(OrderStatusEnum.OS_Touched)).isEqualTo("已挂单");
		assertThat(FieldUtils.chn(OrderStatusEnum.OS_Unknown)).isEqualTo("未知");
	}

	@Test
	public void testIsLong() {
		assertThat(FieldUtils.isLong(PositionDirectionEnum.PD_Long)).isTrue();
		assertThat(FieldUtils.isLong(PositionDirectionEnum.PD_Short)).isFalse();
		assertThat(FieldUtils.isLong(PositionDirectionEnum.PD_Net)).isFalse();
		assertThat(FieldUtils.isLong(PositionDirectionEnum.PD_Unknown)).isFalse();
	}

	@Test
	public void testIsShort() {
		assertThat(FieldUtils.isShort(PositionDirectionEnum.PD_Short)).isTrue();
		assertThat(FieldUtils.isShort(PositionDirectionEnum.PD_Long)).isFalse();
		assertThat(FieldUtils.isShort(PositionDirectionEnum.PD_Net)).isFalse();
		assertThat(FieldUtils.isShort(PositionDirectionEnum.PD_Unknown)).isFalse();
	}

	@Test
	public void testIsBuy() {
		assertThat(FieldUtils.isBuy(DirectionEnum.D_Buy)).isTrue();
		assertThat(FieldUtils.isBuy(DirectionEnum.D_Sell)).isFalse();
		assertThat(FieldUtils.isBuy(DirectionEnum.D_Unknown)).isFalse();
	}

	@Test
	public void testIsSell() {
		assertThat(FieldUtils.isSell(DirectionEnum.D_Buy)).isFalse();
		assertThat(FieldUtils.isSell(DirectionEnum.D_Sell)).isTrue();
		assertThat(FieldUtils.isSell(DirectionEnum.D_Unknown)).isFalse();
	}

	
	@Test
	public void testIsOpposite() {
		assertThat(FieldUtils.isOpposite(DirectionEnum.D_Buy, DirectionEnum.D_Sell)).isTrue();
		assertThat(FieldUtils.isOpposite(DirectionEnum.D_Sell, DirectionEnum.D_Buy)).isTrue();
		assertThat(FieldUtils.isOpposite(DirectionEnum.D_Unknown, DirectionEnum.D_Sell)).isFalse();
		assertThat(FieldUtils.isOpposite(DirectionEnum.D_Buy, DirectionEnum.D_Unknown)).isFalse();
	}
}
