package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import org.dromara.northstar.common.model.DealRecord;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.data.IAccountRepository;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

class SimAccountRemoteTest {

	private MarketCenter mktCenter;
	private IAccountRepository repo;
	private SimTradeGateway gateway;
	private SimAccountRemote account;
	private Contract contract;

	@BeforeEach
	void setUp() {
		mktCenter = mock(MarketCenter.class);
		repo = mock(IAccountRepository.class);
		when(repo.findHoldingTrades(anyString())).thenReturn(HashBasedTable.create());
		when(repo.findDealRecords(anyString())).thenReturn(Collections.emptyList());
		when(repo.findAccountsAsAggregateAccount(anyString())).thenReturn(Account.builder().deposit(1000000).withdraw(900000).available(1000000).build());

		contract = Contract.builder()
				.contractId("sim9901@SHFE@FUTURES@SIM")
				.symbol("sim9901")
				.name("模拟合约9901")
				.multiplier(10)
				.priceTick(1)
				.quantityPrecision(0)
				.contractDefinition(ContractDefinition.builder()
						.commissionFee(6D)
						.marginRatio(0.1)
						.build())
				.build();

		account = new SimAccountRemote(mktCenter, repo);

		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId("TEST_GATEWAY")
				.build();
		gateway = new SimTradeGateway(mock(org.dromara.northstar.common.event.FastEventEngine.class), gd, account, mock(org.dromara.northstar.gateway.IMarketCenter.class));
	}

	@Test
	void testSetGatewayInitializesAccount() {
		assertThat(account.getId()).isEqualTo("TEST_GATEWAY");
		assertThat(account.getTradeGateway()).isEqualTo(gateway);
	}

	@Test
	void testSetGatewayRestoresHoldingPositions() {
		Trade holdingTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(2)
				.build();
		Table<Contract, PositionDirectionEnum, List<Trade>> holdingTable = HashBasedTable.create();
		holdingTable.put(contract, PositionDirectionEnum.PD_Long, List.of(holdingTrade));
		when(repo.findHoldingTrades("TEST_GATEWAY")).thenReturn(holdingTable);

		account.setGateway(gateway);

		assertThat(account.getPosition(PositionDirectionEnum.PD_Long, contract)).isPresent();
		assertThat(account.getPosition(PositionDirectionEnum.PD_Long, contract).get().position()).isEqualTo(2);
	}

	@Test
	void testSetGatewayRestoresDealRecords() {
		DealRecord dr = DealRecord.builder()
				.dealProfit(1000)
				.transactionFee(12)
				.build();
		when(repo.findDealRecords("TEST_GATEWAY")).thenReturn(List.of(dr));

		account.setGateway(gateway);

		// accountBalance = deposit(1000000) - withdraw(900000) + realizedProfit(1000) - commission(12)
		assertThat(account.accountBalance()).isEqualTo(100988);
	}

	@Test
	void testOnOrderPendingOrder() {
		Order order = Order.builder()
				.gatewayId("TEST_GATEWAY")
				.originOrderId("ORDER_1")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.totalVolume(1)
				.orderStatus(OrderStatusEnum.OS_Unknown)
				.build();

		account.onOrder(order);

		assertThat(account.getPendingOrders()).hasSize(1);
	}

	@Test
	void testOnOrderDoneOrderRemovesPending() {
		Order pendingOrder = Order.builder()
				.gatewayId("TEST_GATEWAY")
				.originOrderId("ORDER_1")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.totalVolume(1)
				.orderStatus(OrderStatusEnum.OS_Unknown)
				.build();
		account.onOrder(pendingOrder);
		assertThat(account.getPendingOrders()).hasSize(1);

		Order doneOrder = pendingOrder.toBuilder()
				.orderStatus(OrderStatusEnum.OS_AllTraded)
				.build();
		account.onOrder(doneOrder);

		assertThat(account.getPendingOrders()).isEmpty();
	}

	@Test
	void testOnTradeOpenPosition() {
		Trade trade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(2)
				.build();

		account.onTrade(trade);

		assertThat(account.getPosition(PositionDirectionEnum.PD_Long, contract)).isPresent();
		assertThat(account.getPosition(PositionDirectionEnum.PD_Long, contract).get().position()).isEqualTo(2);
	}

	@Test
	void testOnTradeClosePosition() {
		// Open position first
		Trade openTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(2)
				.build();
		account.onTrade(openTrade);

		// Close position
		Trade closeTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.price(10100)
				.volume(2)
				.build();
		account.onTrade(closeTrade);

		assertThat(account.getPosition(PositionDirectionEnum.PD_Long, contract)).isPresent();
		assertThat(account.getPosition(PositionDirectionEnum.PD_Long, contract).get().position()).isEqualTo(0);
	}

	@Test
	void testSubmitOrderOpenWithInsufficientFundsRejects() {
		when(repo.findAccountsAsAggregateAccount("TEST_GATEWAY")).thenReturn(Account.builder().deposit(0).available(0).build());
		account.setGateway(gateway);

		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(100000)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 0))
				.build();
		when(mktCenter.lastTick(contract)).thenReturn(java.util.Optional.of(tick));

		SubmitOrderReq req = SubmitOrderReq.builder()
				.originOrderId("ORDER_1")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.volume(100)
				.build();

		// Should not throw, but order should be rejected
		account.submitOrder(req);

		assertThat(account.getPendingOrders()).isEmpty();
	}

	@Test
	void testSubmitOrderCloseWithInsufficientPositionRejects() {
		// Open 1 contract
		Trade openTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(1)
				.build();
		account.onTrade(openTrade);

		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(10000)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 0))
				.build();
		when(mktCenter.lastTick(contract)).thenReturn(java.util.Optional.of(tick));

		// Try to close 2 contracts
		SubmitOrderReq req = SubmitOrderReq.builder()
				.originOrderId("ORDER_1")
				.contract(contract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.volume(2)
				.build();

		account.submitOrder(req);

		assertThat(account.getPendingOrders()).isEmpty();
	}

	@Test
	void testSubmitOrderOpenSuccess() {
		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(10000)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 0))
				.build();
		when(mktCenter.lastTick(contract)).thenReturn(java.util.Optional.of(tick));

		SubmitOrderReq req = SubmitOrderReq.builder()
				.originOrderId("ORDER_1")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.volume(1)
				.build();

		account.submitOrder(req);

		// Market order (price=0) should be immediately traded
		assertThat(account.getPendingOrders()).isEmpty();
		assertThat(account.getPosition(PositionDirectionEnum.PD_Long, contract)).isPresent();
		assertThat(account.getPosition(PositionDirectionEnum.PD_Long, contract).get().position()).isEqualTo(1);
	}

	@Test
	void testSubmitOrderLimitPricePending() {
		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(10000)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 0))
				.build();
		when(mktCenter.lastTick(contract)).thenReturn(java.util.Optional.of(tick));

		// Limit price far below market, should not immediately trade
		SubmitOrderReq req = SubmitOrderReq.builder()
				.originOrderId("ORDER_1")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.volume(1)
				.price(9000)
				.build();

		account.submitOrder(req);

		// Should be pending since limit price 9000 < lastPrice 10000, and order is limit price
		assertThat(account.getPendingOrders()).hasSize(1);
	}

	@Test
	void testSubmitOrderCloseSuccess() {
		// Open position
		Trade openTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(2)
				.build();
		account.onTrade(openTrade);

		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(10000)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 0))
				.build();
		when(mktCenter.lastTick(contract)).thenReturn(java.util.Optional.of(tick));

		SubmitOrderReq req = SubmitOrderReq.builder()
				.originOrderId("ORDER_1")
				.contract(contract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.volume(2)
				.build();

		account.submitOrder(req);

		assertThat(account.getPosition(PositionDirectionEnum.PD_Long, contract)).isPresent();
		assertThat(account.getPosition(PositionDirectionEnum.PD_Long, contract).get().position()).isEqualTo(0);
	}

	@Test
	void testCostCalculation() {
		Tick tick = Tick.builder()
				.lastPrice(10000)
				.build();

		SubmitOrderReq req = SubmitOrderReq.builder()
				.contract(contract)
				.volume(2)
				.build();

		double cost = account.cost(req, tick);
		// cost = 10000 * 2 * 10 * 0.1 = 20000
		assertThat(cost).isEqualTo(20000);
	}

	@Test
	void testCancelOrderExistingOrder() {
		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(10000)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 0))
				.build();
		when(mktCenter.lastTick(contract)).thenReturn(java.util.Optional.of(tick));

		// Create a pending order with limit price
		SubmitOrderReq req = SubmitOrderReq.builder()
				.originOrderId("ORDER_1")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.volume(1)
				.price(9000)
				.build();
		account.submitOrder(req);
		assertThat(account.getPendingOrders()).hasSize(1);

		CancelOrderReq cancelReq = CancelOrderReq.builder()
				.originOrderId("ORDER_1")
				.build();

		assertThat(account.cancelOrder(cancelReq)).isTrue();
	}

	@Test
	void testCancelOrderNonExistingOrder() {
		CancelOrderReq cancelReq = CancelOrderReq.builder()
				.originOrderId("ORDER_X")
				.build();

		assertThat(account.cancelOrder(cancelReq)).isFalse();
	}

	@Test
	void testMoneyIODeposit() {
		double balance = account.moneyIO(10000);

		verify(repo).saveAccount(any(Account.class));
		assertThat(balance).isEqualTo(110000);
	}

	@Test
	void testMoneyIOWithdraw() {
		double balance = account.moneyIO(-5000);

		verify(repo).saveAccount(any(Account.class));
		assertThat(balance).isEqualTo(95000);
	}

	@Test
	void testAccountBalanceWithPosition() {
		// Open position
		Trade openTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(1)
				.build();
		account.onTrade(openTrade);

		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(10100)
				.build();
		account.onTick(tick);

		// accountBalance includes unrealized profit from tick update
		assertThat(account.accountBalance()).isEqualTo(101000);
	}

	@Test
	void testAvailableBalance() {
		Trade openTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(1)
				.build();
		account.onTrade(openTrade);

		// available = accountBalance(100000) - margin(10000) = 90000
		assertThat(account.availableBalance()).isEqualTo(90000);
	}

	@Test
	void testGetId() {
		assertThat(account.getId()).isEqualTo("TEST_GATEWAY");
	}

	@Test
	void testGetPositionEmpty() {
		assertThat(account.getPosition(PositionDirectionEnum.PD_Long, contract)).isEmpty();
	}

	@Test
	void testGetPendingOrdersEmpty() {
		assertThat(account.getPendingOrders()).isEmpty();
	}

	@Test
	void testGetTradeGateway() {
		assertThat(account.getTradeGateway()).isEqualTo(gateway);
	}

	@Test
	void testOnTickMatchesBuyLimitOrder() {
		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(10000)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 0))
				.actionTimestamp(System.currentTimeMillis())
				.build();
		when(mktCenter.lastTick(contract)).thenReturn(java.util.Optional.of(tick));

		// Create limit buy order at 10001
		SubmitOrderReq req = SubmitOrderReq.builder()
				.originOrderId("ORDER_1")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.volume(1)
				.price(10001)
				.build();
		account.submitOrder(req);
		assertThat(account.getPendingOrders()).hasSize(1);

		// Tick below limit price should match for buy
		Tick newTick = Tick.builder()
				.contract(contract)
				.lastPrice(10000)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 1))
				.actionTimestamp(System.currentTimeMillis())
				.build();
		account.onTick(newTick);

		assertThat(account.getPendingOrders()).isEmpty();
		assertThat(account.getPosition(PositionDirectionEnum.PD_Long, contract)).isPresent();
	}

	@Test
	void testOnTickMatchesSellLimitOrder() {
		// Open position first
		Trade openTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(1)
				.build();
		account.onTrade(openTrade);

		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(10000)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 0))
				.actionTimestamp(System.currentTimeMillis())
				.build();
		when(mktCenter.lastTick(contract)).thenReturn(java.util.Optional.of(tick));

		// Create limit sell order at 9999
		SubmitOrderReq req = SubmitOrderReq.builder()
				.originOrderId("ORDER_1")
				.contract(contract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.volume(1)
				.price(9999)
				.build();
		account.submitOrder(req);
		assertThat(account.getPendingOrders()).hasSize(1);

		// Tick above limit price should match for sell
		Tick newTick = Tick.builder()
				.contract(contract)
				.lastPrice(10000)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 1))
				.actionTimestamp(System.currentTimeMillis())
				.build();
		account.onTick(newTick);

		assertThat(account.getPendingOrders()).isEmpty();
		assertThat(account.getPosition(PositionDirectionEnum.PD_Long, contract)).isPresent();
		assertThat(account.getPosition(PositionDirectionEnum.PD_Long, contract).get().position()).isEqualTo(0);
	}

	@Test
	void testOnTickDoesNotMatchBuyLimitOrder() {
		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(10000)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 0))
				.actionTimestamp(System.currentTimeMillis())
				.build();
		when(mktCenter.lastTick(contract)).thenReturn(java.util.Optional.of(tick));

		// Create limit buy order at 10050 (below current price)
		SubmitOrderReq req = SubmitOrderReq.builder()
				.originOrderId("ORDER_1")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.volume(1)
				.price(10050)
				.build();
		account.submitOrder(req);
		assertThat(account.getPendingOrders()).hasSize(1);

		// Tick above limit price should not match for buy
		Tick newTick = Tick.builder()
				.contract(contract)
				.lastPrice(10100)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 1))
				.actionTimestamp(System.currentTimeMillis())
				.build();
		account.onTick(newTick);

		assertThat(account.getPendingOrders()).hasSize(1);
	}

	@Test
	void testOnTickDoesNotMatchSellLimitOrder() {
		// Open position
		Trade openTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(1)
				.build();
		account.onTrade(openTrade);

		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(10000)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 0))
				.actionTimestamp(System.currentTimeMillis())
				.build();
		when(mktCenter.lastTick(contract)).thenReturn(java.util.Optional.of(tick));

		// Create limit sell order at 10050 (above current price)
		SubmitOrderReq req = SubmitOrderReq.builder()
				.originOrderId("ORDER_1")
				.contract(contract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.volume(1)
				.price(10050)
				.build();
		account.submitOrder(req);
		assertThat(account.getPendingOrders()).hasSize(1);

		// Tick below limit price should not match for sell
		Tick newTick = Tick.builder()
				.contract(contract)
				.lastPrice(10000)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 1))
				.actionTimestamp(System.currentTimeMillis())
				.build();
		account.onTick(newTick);

		assertThat(account.getPendingOrders()).hasSize(1);
	}

	@Test
	void testOnTickIgnoresDifferentContract() {
		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(10000)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 0))
				.actionTimestamp(System.currentTimeMillis())
				.build();
		when(mktCenter.lastTick(contract)).thenReturn(java.util.Optional.of(tick));

		SubmitOrderReq req = SubmitOrderReq.builder()
				.originOrderId("ORDER_1")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.volume(1)
				.price(10000)
				.build();
		account.submitOrder(req);
		assertThat(account.getPendingOrders()).hasSize(1);

		Contract otherContract = Contract.builder()
				.contractId("other@SHFE@FUTURES@SIM")
				.symbol("other")
				.build();
		Tick otherTick = Tick.builder()
				.contract(otherContract)
				.lastPrice(10000)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 1))
				.build();
		account.onTick(otherTick);

		assertThat(account.getPendingOrders()).hasSize(1);
	}

	@Test
	void testOnTickMatchesAnyPriceOrder() {
		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(10000)
				.tradingDay(LocalDate.of(2024, 1, 1))
				.actionDay(LocalDate.of(2024, 1, 1))
				.actionTime(LocalTime.of(10, 0, 0))
				.actionTimestamp(System.currentTimeMillis())
				.build();
		when(mktCenter.lastTick(contract)).thenReturn(java.util.Optional.of(tick));

		// price=0 means any price
		SubmitOrderReq req = SubmitOrderReq.builder()
				.originOrderId("ORDER_1")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.volume(1)
				.price(0)
				.build();
		account.submitOrder(req);

		// Any price order should be immediately traded in submitOrder
		assertThat(account.getPendingOrders()).isEmpty();
	}
}
