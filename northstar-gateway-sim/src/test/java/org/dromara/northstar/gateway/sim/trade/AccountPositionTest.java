package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.dromara.northstar.common.model.DealRecord;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.data.IAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

class AccountPositionTest {

	private IAccountRepository repo;
	private Contract contract;
	private AccountPosition longPosition;
	private AccountPosition shortPosition;

	@BeforeEach
	void setUp() {
		repo = mock(IAccountRepository.class);
		
		contract = Contract.builder()
				.contractId("sim9901@SHFE@FUTURES@SIM")
				.symbol("sim9901")
				.name("模拟合约9901")
				.multiplier(10)
				.priceTick(1)
				.contractDefinition(ContractDefinition.builder()
						.commissionFee(6D)
						.marginRatio(0.1)
						.build())
				.build();
		
		longPosition = new AccountPosition("TEST_GATEWAY", contract, PositionDirectionEnum.PD_Long, repo);
		shortPosition = new AccountPosition("TEST_GATEWAY", contract, PositionDirectionEnum.PD_Short, repo);
	}

	@Test
	void testInitialState() {
		assertThat(longPosition.position()).isEqualTo(0);
		assertThat(longPosition.frozen()).isEqualTo(0.0);
		assertThat(longPosition.avgOpenPrice()).isEqualTo(0);
		assertThat(longPosition.profit()).isEqualTo(0);
		assertThat(longPosition.margin()).isEqualTo(0);
		assertThat(longPosition.getAccountName()).isEqualTo("TEST_GATEWAY");
		assertThat(longPosition.getDirection()).isEqualTo(PositionDirectionEnum.PD_Long);
		assertThat(longPosition.getContract()).isEqualTo(contract);
	}

	@Test
	void testOnTickUpdatesLastTick() {
		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(10500)
				.build();
		
		longPosition.onTick(tick);
		
		// No position yet, profit should still be 0
		assertThat(longPosition.profit()).isEqualTo(0);
	}

	@Test
	void testOnTickIgnoresDifferentContract() {
		Contract otherContract = Contract.builder()
				.contractId("other@SHFE@FUTURES@SIM")
				.symbol("other")
				.build();
		
		Tick tick = Tick.builder()
				.contract(otherContract)
				.lastPrice(10500)
				.build();
		
		longPosition.onTick(tick);
		// Should not throw and should not update lastTick
		assertThat(longPosition.profit()).isEqualTo(0);
	}

	@Test
	void testOpenLongTrade() {
		Trade trade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(2)
				.build();
		
		longPosition.onTrade(trade);
		
		assertThat(longPosition.position()).isEqualTo(2);
		assertThat(longPosition.avgOpenPrice()).isEqualTo(10000);
		assertThat(longPosition.getOpeningTrades()).hasSize(1);
	}

	@Test
	void testOpenShortTradeIgnoredByLongPosition() {
		Trade trade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(2)
				.build();
		
		longPosition.onTrade(trade);
		
		assertThat(longPosition.position()).isEqualTo(0);
	}

	@Test
	void testCloseLongTrade() {
		// First open a position
		Trade openTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(2)
				.build();
		longPosition.onTrade(openTrade);
		
		// Then close it
		Trade closeTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.price(10100)
				.volume(2)
				.build();
		longPosition.onTrade(closeTrade);
		
		assertThat(longPosition.position()).isEqualTo(0);
		// Profit = (10100 - 10000) * 2 * 10 = 2000
		assertThat(longPosition.getAccRealizedProfit()).isEqualTo(2000);
		// Commission = 6 * 2 * 2 = 24 (fee * 2 sides * volume)
		assertThat(longPosition.getAccCommission()).isEqualTo(24);
		verify(repo).saveDealRecord(any(DealRecord.class));
	}

	@Test
	void testPartialClose() {
		// Open 3 contracts
		Trade openTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(3)
				.build();
		longPosition.onTrade(openTrade);
		
		// Close 1 contract
		Trade closeTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.price(10100)
				.volume(1)
				.build();
		longPosition.onTrade(closeTrade);
		
		assertThat(longPosition.position()).isEqualTo(2);
		assertThat(longPosition.avgOpenPrice()).isEqualTo(10000);
		// Profit = (10100 - 10000) * 1 * 10 = 1000
		assertThat(longPosition.getAccRealizedProfit()).isEqualTo(1000);
	}

	@Test
	void testCloseMoreThanOpen() {
		// Open 1 contract
		Trade openTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(1)
				.build();
		longPosition.onTrade(openTrade);
		
		// Close 2 contracts (more than open)
		Trade closeTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.price(10100)
				.volume(2)
				.build();
		longPosition.onTrade(closeTrade);
		
		assertThat(longPosition.position()).isEqualTo(0);
	}

	@Test
	void testPartialCloseAcrossMultipleOpenTrades() {
		// Open 5 contracts at 10000
		Trade openTrade1 = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(5)
				.build();
		longPosition.onTrade(openTrade1);
		
		// Open 2 contracts at 10200
		Trade openTrade2 = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10200)
				.volume(2)
				.build();
		longPosition.onTrade(openTrade2);
		
		assertThat(longPosition.position()).isEqualTo(7);
		assertThat(longPosition.avgOpenPrice()).isEqualTo((10000*5+10200*2) / 7.0);
		
		// Close 6 contracts at 10100
		// Expected: 5 contracts from openTrade1 fully closed, 1 from openTrade2 partially closed
		// Profit = (10100 - 10000) * 5 * 10 + (10100 - 10200) * 1 * 10 = 5000 - 1000 = 4000
		Trade closeTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.price(10100)
				.volume(6)
				.build();
		longPosition.onTrade(closeTrade);
		
		assertThat(longPosition.position()).isEqualTo(1);
		assertThat(longPosition.avgOpenPrice()).isEqualTo(10200);
		assertThat(longPosition.getAccRealizedProfit()).isEqualTo(4000);
	}

	@Test
	void testMultipleOpenTradesAvgPrice() {
		Trade trade1 = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(1)
				.build();
		Trade trade2 = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10200)
				.volume(1)
				.build();
		
		longPosition.onTrade(trade1);
		longPosition.onTrade(trade2);
		
		assertThat(longPosition.position()).isEqualTo(2);
		assertThat(longPosition.avgOpenPrice()).isEqualTo(10100);
	}

	@Test
	void testProfitCalculation() {
		Trade openTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(1)
				.build();
		longPosition.onTrade(openTrade);
		
		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(10100)
				.build();
		longPosition.onTick(tick);
		
		// Profit for long = (10100 - 10000) * 1 * 10 = 1000
		assertThat(longPosition.profit()).isEqualTo(1000);
	}

	@Test
	void testShortProfitCalculation() {
		Trade openTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(1)
				.build();
		shortPosition.onTrade(openTrade);
		
		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(9900)
				.build();
		shortPosition.onTick(tick);
		
		// Profit for short = (10000 - 9900) * 1 * 10 = 1000
		assertThat(shortPosition.profit()).isEqualTo(1000);
	}

	@Test
	void testMarginCalculation() {
		Trade openTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(2)
				.build();
		longPosition.onTrade(openTrade);
		
		// Margin = 2 * 10000 * 10 * 0.1 = 20000
		assertThat(longPosition.margin()).isEqualTo(20000);
	}

	@Test
	void testOnOrderForCloseOrder() {
		Order order = Order.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.totalVolume(1)
				.orderStatus(OrderStatusEnum.OS_Unknown)
				.build();
		
		longPosition.onOrder(order);
		
		assertThat(longPosition.frozen()).isEqualTo(1.0);
	}

	@Test
	void testOnOrderForOpenOrderIgnored() {
		Order order = Order.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.totalVolume(1)
				.orderStatus(OrderStatusEnum.OS_Unknown)
				.build();
		
		longPosition.onOrder(order);
		
		assertThat(longPosition.frozen()).isEqualTo(0.0);
	}

	@Test
	void testOnOrderForRejectedOrder() {
		Order order = Order.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.totalVolume(1)
				.orderStatus(OrderStatusEnum.OS_Rejected)
				.build();
		
		longPosition.onOrder(order);
		
		assertThat(longPosition.frozen()).isEqualTo(0.0);
	}

	@Test
	void testOnOrderForCanceledOrderRemovesFrozen() {
		Order order = Order.builder()
				.gatewayId("TEST_GATEWAY")
				.originOrderId("ORDER_1")
				.contract(contract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.totalVolume(1)
				.orderStatus(OrderStatusEnum.OS_Unknown)
				.build();
		
		longPosition.onOrder(order);
		assertThat(longPosition.frozen()).isEqualTo(1.0);
		
		Order canceledOrder = order.toBuilder()
				.orderStatus(OrderStatusEnum.OS_Canceled)
				.build();
		longPosition.onOrder(canceledOrder);
		
		assertThat(longPosition.frozen()).isEqualTo(0.0);
	}

	@Test
	void testOnOrderIgnoresDifferentContract() {
		Contract otherContract = Contract.builder()
				.contractId("other@SHFE@FUTURES@SIM")
				.symbol("other")
				.build();
		
		Order order = Order.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(otherContract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.totalVolume(1)
				.orderStatus(OrderStatusEnum.OS_Unknown)
				.build();
		
		longPosition.onOrder(order);
		assertThat(longPosition.frozen()).isEqualTo(0.0);
	}

	@Test
	void testConvertToPosition() {
		Trade openTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(2)
				.build();
		longPosition.onTrade(openTrade);
		
		Tick tick = Tick.builder()
				.contract(contract)
				.lastPrice(10100)
				.build();
		longPosition.onTick(tick);
		
		Position position = longPosition.convertToPosition();
		
		assertThat(position).isNotNull();
		assertThat(position.position()).isEqualTo(2);
		assertThat(position.openPrice()).isEqualTo(10000);
		assertThat(position.positionProfit()).isEqualTo(2000);
		assertThat(position.useMargin()).isEqualTo(20000);
		assertThat(position.exchangeMargin()).isEqualTo(20000);
		assertThat(position.positionDirection()).isEqualTo(PositionDirectionEnum.PD_Long);
		assertThat(position.contract()).isEqualTo(contract);
		assertThat(position.lastTick()).isEqualTo(tick);
	}

	@Test
	void testConvertToPositionWithoutTick() {
		Trade openTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(2)
				.build();
		longPosition.onTrade(openTrade);
		
		Position position = longPosition.convertToPosition();
		
		assertThat(position.positionProfit()).isEqualTo(0);
		assertThat(position.openPriceDiff()).isEqualTo(0);
	}

	@Test
	void testHoldingTradesLoadedFromRepo() {
		Trade holdingTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(10000)
				.volume(1)
				.build();
		
		AccountPosition pos = new AccountPosition("TEST_GATEWAY", contract, PositionDirectionEnum.PD_Long, repo, List.of(holdingTrade));
		
		assertThat(pos.position()).isEqualTo(1);
		assertThat(pos.avgOpenPrice()).isEqualTo(10000);
	}

	@Test
	void testCommissionRateBasedCalculation() {
		Contract rateContract = Contract.builder()
				.contractId("sim9903@BINANCE@FUTURES@SIM")
				.symbol("sim9903")
				.name("模拟合约9903")
				.multiplier(1)
				.priceTick(1.0E-7)
				.contractDefinition(ContractDefinition.builder()
						.commissionRate(3.0E-4)
						.marginRatio(0.1)
						.build())
				.build();
		
		AccountPosition pos = new AccountPosition("TEST_GATEWAY", rateContract, PositionDirectionEnum.PD_Long, repo);
		
		Trade openTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(rateContract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(50000)
				.volume(1)
				.build();
		pos.onTrade(openTrade);
		
		Trade closeTrade = Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(rateContract)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.price(51000)
				.volume(1)
				.build();
		pos.onTrade(closeTrade);
		
		// Profit = (51000 - 50000) * 1 * 1 = 1000
		assertThat(pos.getAccRealizedProfit()).isEqualTo(1000);
		// Commission = 3.0E-4 * 51000 * 1 * 2 = 30.6
		assertThat(pos.getAccCommission()).isCloseTo(30.6, org.assertj.core.data.Offset.offset(0.001));
	}
}
