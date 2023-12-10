package org.dromara.northstar.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.strategy.IModuleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;

class ModuleStateMachineTest {

	ContractDefinition cd = ContractDefinition.builder().commissionFee(0).build();
	Contract contract = Contract.builder().unifiedSymbol("rb2205@SHFE@FUTURES").contractDefinition(cd).multiplier(10).longMarginRatio(0.08).shortMarginRatio(0.08).build();
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
	
	IModuleContext ctx = mock(IModuleContext.class);
	
	ModuleAccount macc = mock(ModuleAccount.class);
	
	@BeforeEach
	void prepare() {
		when(ctx.getLogger()).thenReturn(mock(Logger.class));
	}
	
	@Test
	void shouldGetEmpty() {
		ModuleStateMachine msm = new ModuleStateMachine(ctx);
		assertThat(msm.getState()).isEqualTo(ModuleState.EMPTY);
	}

	@Test
	void shouldGetPlacingOrder() {
		ModuleStateMachine msm = new ModuleStateMachine(ctx);
		msm.onSubmitReq();
		assertThat(msm.getState()).isEqualTo(ModuleState.PLACING_ORDER);
	}
	
	@Test
	void shouldGetPendingOrder() {
		ModuleStateMachine msm = new ModuleStateMachine(ctx);
		msm.onSubmitReq();
		msm.onOrder(order);
		assertThat(msm.getState()).isEqualTo(ModuleState.PENDING_ORDER);
		msm.onOrder(order2a);
		assertThat(msm.getState()).isEqualTo(ModuleState.PENDING_ORDER);
		msm.onOrder(order2b);
		assertThat(msm.getState()).isEqualTo(ModuleState.PENDING_ORDER);
	}
	
	@Test
	void shouldGetLong() {
		when(macc.getNonclosedTrades()).thenReturn(List.of(trade));
		ModuleStateMachine msm = new ModuleStateMachine(ctx);
		msm.setModuleAccount(macc);
		msm.onSubmitReq();
		msm.onOrder(order);
		msm.onOrder(order2);
		msm.onTrade(trade);
		assertThat(msm.getState()).isEqualTo(ModuleState.HOLDING_LONG);
	}
	
	@Test
	void shouldGetShort() {
		when(macc.getNonclosedTrades()).thenReturn(List.of(trade2));
		ModuleStateMachine msm = new ModuleStateMachine(ctx);
		msm.setModuleAccount(macc);
		msm.onSubmitReq();
		msm.onOrder(order4);
		msm.onOrder(order5);
		msm.onTrade(trade2);
		assertThat(msm.getState()).isEqualTo(ModuleState.HOLDING_SHORT);
	}
	
	@Test
	void shouldGetHedgeEmpty() {
		when(macc.getNonclosedTrades()).thenReturn(List.of(trade));
		ModuleStateMachine msm = new ModuleStateMachine(ctx);
		msm.setModuleAccount(macc);
		msm.onSubmitReq();
		msm.onOrder(order);
		msm.onOrder(order2);
		msm.onTrade(trade);
		assertThat(msm.getState()).isEqualTo(ModuleState.HOLDING_LONG);
		
		when(macc.getNonclosedTrades()).thenReturn(List.of(trade, trade2));
		msm.onSubmitReq();
		msm.onOrder(order4);
		msm.onOrder(order5);
		msm.onTrade(trade2);
		assertThat(msm.getState()).isEqualTo(ModuleState.EMPTY_HEDGE);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	void shouldGetHedgeHolding() {
		Trade trade3 = Trade.builder()
				.contract(contract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(1000)
				.volume(3)
				.build();
		when(macc.getNonclosedTrades()).thenReturn(List.of(trade), List.of(trade, trade3));
		ModuleStateMachine msm = new ModuleStateMachine(ctx);
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
	
	@Test
	void shouldGetCancelling() {
		when(macc.getNonclosedTrades()).thenReturn(List.of());
		ModuleStateMachine msm = new ModuleStateMachine(ctx);
		msm.setModuleAccount(macc);
		msm.onSubmitReq();
		msm.onOrder(order);
		msm.onOrder(order3);
		assertThat(msm.getState()).isEqualTo(ModuleState.EMPTY);
	}
}
