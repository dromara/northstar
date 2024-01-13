package org.dromara.northstar.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleAccountDescription;
import org.dromara.northstar.common.model.ModuleAccountRuntimeDescription;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModulePositionDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.strategy.IModuleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import xyz.redtorch.pb.CoreEnum;

class ModuleAccountTest {

	ContractDefinition cd = ContractDefinition.builder().commissionFee(0).build();
	Contract contract = Contract.builder().unifiedSymbol("rb2205@SHFE@FUTURES").contractDefinition(cd).multiplier(10).build();
	Order order = Order.builder()
			.contract(contract)
			.direction(CoreEnum.DirectionEnum.D_Buy)
			.offsetFlag(CoreEnum.OffsetFlagEnum.OF_Open)
			.price(1000)
			.totalVolume(2)
			.orderStatus(CoreEnum.OrderStatusEnum.OS_AllTraded)
			.build();
	Order order2 = Order.builder()
			.contract(contract)
			.direction(CoreEnum.DirectionEnum.D_Sell)
			.offsetFlag(CoreEnum.OffsetFlagEnum.OF_Open)
			.price(1000)
			.totalVolume(2)
			.orderStatus(CoreEnum.OrderStatusEnum.OS_AllTraded)
			.build();
	Trade trade = Trade.builder()
			.contract(contract)
			.direction(CoreEnum.DirectionEnum.D_Buy)
			.offsetFlag(CoreEnum.OffsetFlagEnum.OF_Open)
			.price(1000)
			.volume(2)
			.tradeDate(LocalDate.now())
			.tradingDay(LocalDate.now())
			.tradeTime(LocalTime.now())
			.build();
	Trade trade2 = Trade.builder()
			.contract(contract)
			.direction(CoreEnum.DirectionEnum.D_Sell)
			.offsetFlag(CoreEnum.OffsetFlagEnum.OF_Open)
			.price(1000)
			.volume(2)
			.tradeDate(LocalDate.now())
			.tradingDay(LocalDate.now())
			.tradeTime(LocalTime.now())
			.build();
	Trade closeTrade = Trade.builder()
			.contract(contract)
			.direction(CoreEnum.DirectionEnum.D_Sell)
			.offsetFlag(CoreEnum.OffsetFlagEnum.OF_Close)
			.price(1200)
			.volume(2)
			.tradeDate(LocalDate.now())
			.tradingDay(LocalDate.now())
			.tradeTime(LocalTime.now())
			.build();
	Tick tick = Tick.builder()
			.contract(contract)
			.tradingDay(LocalDate.now())
			.lastPrice(1000)
			.build();

	IModuleRepository moduleRepo = mock(IModuleRepository.class);
	IModuleContext ctx = mock(IModuleContext.class);

	ContractSimpleInfo csi = ContractSimpleInfo.builder()
			.unifiedSymbol("rb2205@SHFE@FUTURES")
			.value("rb2205@SHFE@FUTURES@testAccount")
			.build();

	ModuleAccount macc;

	@BeforeEach
	void prepare() {
		ModuleAccountDescription mad = ModuleAccountDescription.builder()
				.accountGatewayId("testAccount")
				.bindedContracts(List.of(csi))
				.build();

		ModuleDescription md = ModuleDescription.builder()
				.closingPolicy(ClosingPolicy.FIRST_IN_FIRST_OUT)
				.initBalance(100000)
				.moduleAccountSettingsDescription(List.of(mad))
				.build();

		ModuleAccountRuntimeDescription mard = ModuleAccountRuntimeDescription.builder()
				.initBalance(100000)
				.accCloseProfit(200)
				.accCommission(10)
				.accDealVolume(3)
				.positionDescription(ModulePositionDescription.builder()
						.nonclosedTrades(List.of(trade, trade2).stream().map(t -> t.toTradeField().toByteArray()).toList())
						.build())
				.build();

		ModuleRuntimeDescription mrd = ModuleRuntimeDescription.builder()
				.moduleName("testModule")
				.enabled(true)
				.moduleState(ModuleState.HOLDING_LONG)
				.moduleAccountRuntime(mard)
				.build();
		IContractManager contractMgr = mock(IContractManager.class);
		IContract c = mock(IContract.class);
		when(c.contract()).thenReturn(trade.contract());
		when(contractMgr.getContract(any(Identifier.class))).thenReturn(c);
		when(ctx.getLogger(any())).thenReturn(mock(Logger.class));
		macc = new ModuleAccount(md, mrd, new ModuleStateMachine(ctx), moduleRepo, contractMgr, ctx);
	}

	@Test
	void testGetModuleStateMachine() {
		assertThat(macc.getModuleState()).isEqualTo(ModuleState.EMPTY_HEDGE);
	}

	@Test
	void testOnTrade() {
		macc.onOrder(order);
		macc.onTrade(closeTrade);
		assertThat(macc.getModuleState()).isEqualTo(ModuleState.HOLDING_SHORT);
		assertThat(macc.getInitBalance()).isEqualTo(100000);
		assertThat(macc.getAccCloseProfit()).isEqualTo(4200);
		assertThat(macc.getAccDealVolume()).isEqualTo(5);
		assertThat(macc.getNonclosedTrades()).hasSize(1);
	}

	@Test
	void testGetInitBalance() {
		assertThat(macc.getInitBalance()).isEqualTo(100000);
	}

	@Test
	void testGetUncloseTrades() {
		assertThat(macc.getNonclosedTrades()).hasSize(2);
	}

	@Test
	void testGetAccDealVolume() {
		assertThat(macc.getAccDealVolume()).isEqualTo(3);
	}

	@Test
	void testGetAccCloseProfit() {
		assertThat(macc.getAccCloseProfit()).isEqualTo(200);
	}

	@Test
	void testGetPositions() {
		macc.onTick(tick);
		assertThat(macc.getPositions()).hasSize(2);
	}

}
