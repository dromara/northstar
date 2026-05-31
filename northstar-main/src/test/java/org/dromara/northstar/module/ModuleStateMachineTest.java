package org.dromara.northstar.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.strategy.IModuleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

class ModuleStateMachineTest {

	ContractDefinition cd = ContractDefinition.builder().commissionFee(0D).marginRatio(0.08).build();
	Contract contract = Contract.builder().unifiedSymbol("rb2205@SHFE@FUTURES").contractDefinition(cd).multiplier(10).build();
	Contract contract2 = Contract.builder().unifiedSymbol("rb2206@SHFE@FUTURES").contractDefinition(cd).multiplier(10).build();
	SubmitOrderReq orderReq = SubmitOrderReq.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.price(1000)
			.volume(2)
			.build();
	SubmitOrderReq orderReq2 = SubmitOrderReq.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Sell)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.price(1000)
			.volume(2)
			.build();
	Order order = Order.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.price(1000)
			.totalVolume(2)
			.orderStatus(OrderStatusEnum.OS_Unknown)
			.build();
	Order order2a = Order.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.price(1000)
			.totalVolume(2)
			.orderStatus(OrderStatusEnum.OS_PartTradedNotQueueing)
			.build();
	Order order2b = Order.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.price(1000)
			.totalVolume(2)
			.orderStatus(OrderStatusEnum.OS_PartTradedQueueing)
			.build();
	Order order2 = Order.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.price(1000)
			.totalVolume(2)
			.orderStatus(OrderStatusEnum.OS_AllTraded)
			.build();
	Order order3 = Order.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.price(1000)
			.totalVolume(2)
			.orderStatus(OrderStatusEnum.OS_Canceled)
			.build();
	Order order4 = Order.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Sell)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.price(1000)
			.totalVolume(2)
			.orderStatus(OrderStatusEnum.OS_Touched)
			.build();
	Order order5 = Order.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Sell)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.price(1000)
			.totalVolume(2)
			.orderStatus(OrderStatusEnum.OS_AllTraded)
			.build();
	Trade trade = Trade.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.price(1000)
			.volume(2)
			.build();
	Trade trade2 = Trade.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Sell)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.price(1000)
			.volume(2)
			.build();
	
	Position longPos = Position.builder()
			.contract(contract)
			.positionDirection(PositionDirectionEnum.PD_Long)
			.position(2)
			.build();
	Position shortPos = Position.builder()
			.contract(contract)
			.positionDirection(PositionDirectionEnum.PD_Short)
			.position(2)
			.build();
	Position shortPosDiffContract = Position.builder()
			.contract(contract2)
			.positionDirection(PositionDirectionEnum.PD_Short)
			.position(2)
			.build();
	
	IModuleContext ctx = mock(IModuleContext.class);
	
	ModuleAccount macc = mock(ModuleAccount.class);
	
	@BeforeEach
	void prepare() {
		when(ctx.getLogger(any())).thenReturn(mock(Logger.class));
		
	}
	
	@Test
	void shouldGetEmpty() {
		ModuleStateMachine msm = new ModuleStateMachine();
		msm.setLogger(mock(Logger.class));
		assertThat(msm.getState()).isEqualTo(ModuleState.EMPTY);
	}

	@Test
	void shouldGetPlacingOrder() {
		ModuleStateMachine msm = new ModuleStateMachine();
		msm.setLogger(mock(Logger.class));
		msm.onSubmitReq();
		assertThat(msm.getState()).isEqualTo(ModuleState.PLACING_ORDER);
	}
	
	@Test
	void shouldGetPendingOrder() {
		ModuleStateMachine msm = new ModuleStateMachine();
		msm.setLogger(mock(Logger.class));
		msm.onSubmitReq();
		msm.onOrder(order);
		assertThat(msm.getState()).isEqualTo(ModuleState.PENDING_ORDER);
		msm.onOrder(order2a);
		assertThat(msm.getState()).isEqualTo(ModuleState.PENDING_ORDER);
		msm.onOrder(order2b);
		assertThat(msm.getState()).isEqualTo(ModuleState.PENDING_ORDER);
	}
	
	@Test
	void shouldRestoreStateOnFailSubmit() {
		ModuleStateMachine msm = new ModuleStateMachine();
		msm.setLogger(mock(Logger.class));
		msm.onSubmitReq();
		assertThat(msm.getState()).isEqualTo(ModuleState.PLACING_ORDER);
		msm.onFailSubmitReq();
		assertThat(msm.getState()).isEqualTo(ModuleState.EMPTY);
	}
	
	@Test
	void shouldThrowWhenOrderingAndSubmitAgain() {
		ModuleStateMachine msm = new ModuleStateMachine();
		msm.setLogger(mock(Logger.class));
		msm.onSubmitReq();
		assertThatThrownBy(() -> msm.onSubmitReq())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("不能继续下单");
	}
	
	@Test
	void shouldGetLong() {
		when(macc.getPositions()).thenReturn(List.of(longPos));
		ModuleStateMachine msm = new ModuleStateMachine();
		msm.setLogger(mock(Logger.class));
		msm.setModuleAccount(macc);
		msm.onSubmitReq();
		msm.onOrder(order);
		msm.onOrder(order2);
		msm.onTrade(trade);
		assertThat(msm.getState()).isEqualTo(ModuleState.HOLDING_LONG);
	}
	
	@Test
	void shouldGetShort() {
		when(macc.getPositions()).thenReturn(List.of(shortPos));
		ModuleStateMachine msm = new ModuleStateMachine();
		msm.setLogger(mock(Logger.class));
		msm.setModuleAccount(macc);
		msm.onSubmitReq();
		msm.onOrder(order4);
		msm.onOrder(order5);
		msm.onTrade(trade2);
		assertThat(msm.getState()).isEqualTo(ModuleState.HOLDING_SHORT);
	}
	
	@Test
	void shouldGetHedgeEmpty() {
		when(macc.getPositions()).thenReturn(List.of(longPos));
		ModuleStateMachine msm = new ModuleStateMachine();
		msm.setLogger(mock(Logger.class));
		msm.setModuleAccount(macc);
		msm.onSubmitReq();
		msm.onOrder(order);
		msm.onOrder(order2);
		msm.onTrade(trade);
		assertThat(msm.getState()).isEqualTo(ModuleState.HOLDING_LONG);
		
		when(macc.getPositions()).thenReturn(List.of(longPos, shortPos));
		msm.onSubmitReq();
		msm.onOrder(order4);
		msm.onOrder(order5);
		msm.onTrade(trade2);
		assertThat(msm.getState()).isEqualTo(ModuleState.EMPTY_HEDGE);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	void shouldGetHedgeHolding() {
		when(macc.getPositions()).thenReturn(List.of(longPos), List.of(longPos, shortPosDiffContract));
		ModuleStateMachine msm = new ModuleStateMachine();
		msm.setLogger(mock(Logger.class));
		msm.setModuleAccount(macc);
		msm.onSubmitReq();
		msm.onOrder(order);
		msm.onOrder(order2);
		msm.onTrade(trade);
		msm.onOrder(order4);
		msm.onOrder(order5);
		msm.onTrade(trade2);
		assertThat(msm.getState()).isEqualTo(ModuleState.HOLDING_HEDGE);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	void shouldGetErrorWhenUnequalHedge() {
		Position shortPos3 = Position.builder()
				.contract(contract)
				.positionDirection(PositionDirectionEnum.PD_Short)
				.position(3)
				.build();
		when(macc.getPositions()).thenReturn(List.of(longPos), List.of(longPos, shortPos3));
		ModuleStateMachine msm = new ModuleStateMachine();
		msm.setLogger(mock(Logger.class));
		msm.setModuleAccount(macc);
		msm.onSubmitReq();
		msm.onOrder(order);
		msm.onOrder(order2);
		msm.onTrade(trade);
		msm.onOrder(order4);
		msm.onOrder(order5);
		msm.onTrade(trade2);
		assertThat(msm.getState()).isEqualTo(ModuleState.ERROR);
	}
	
	@Test
	void shouldGetCancelling() {
		when(macc.getPositions()).thenReturn(List.of());
		ModuleStateMachine msm = new ModuleStateMachine();
		msm.setLogger(mock(Logger.class));
		msm.setModuleAccount(macc);
		msm.onSubmitReq();
		msm.onOrder(order);
		msm.onOrder(order3);
		assertThat(msm.getState()).isEqualTo(ModuleState.EMPTY);
	}
	
	@Test
	void shouldUpdateStateOnRejectedOrder() {
		when(macc.getPositions()).thenReturn(List.of());
		ModuleStateMachine msm = new ModuleStateMachine();
		msm.setLogger(mock(Logger.class));
		msm.setModuleAccount(macc);
		msm.onSubmitReq();
		Order rejectedOrder = Order.builder()
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(1000)
				.totalVolume(2)
				.orderStatus(OrderStatusEnum.OS_Rejected)
				.build();
		msm.onOrder(rejectedOrder);
		assertThat(msm.getState()).isEqualTo(ModuleState.EMPTY);
	}
}
