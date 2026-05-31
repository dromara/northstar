package org.dromara.northstar.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.dromara.northstar.common.exception.IllegalOrderRequestException;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.dromara.northstar.data.IAccountRepository;
import org.dromara.northstar.gateway.sim.trade.AccountPosition;
import org.dromara.northstar.strategy.IAccount;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.context.MessageSource;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

class ModuleAccountTest {

	ContractDefinition cd = ContractDefinition.builder().commissionFee(0D).marginRatio(0.08).build();
	Contract contract = Contract.builder().unifiedSymbol("rb2205@SHFE@FUTURES").contractDefinition(cd).multiplier(10).build();

	IAccountRepository repo = mock(IAccountRepository.class);
	Logger logger = mock(Logger.class);
	IAccount tradeAccount = mock(IAccount.class);

	ModuleAccount macc;
	Map<Contract, IAccount> contractAccountMap;
	
	Trade openTrade = Trade.builder()
			.contract(contract)
			.direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.price(1000)
			.volume(2)
			.tradeDate(LocalDate.now())
			.tradingDay(LocalDate.now())
			.tradeTime(LocalTime.now())
			.build();
	
	@BeforeAll
    static void initMessageSource() {
        MessageSourceHolder.setMessageSource(mock(MessageSource.class));
    }

	@BeforeEach
	void prepare() {
		contractAccountMap = new HashMap<>();
		contractAccountMap.put(contract, tradeAccount);
		when(tradeAccount.getId()).thenReturn("testAccount");
		
	}
	
	private Table<Contract, PositionDirectionEnum, AccountPosition> createPositionTable(AccountPosition... positions) {
		Table<Contract, PositionDirectionEnum, AccountPosition> table = HashBasedTable.create();
		for (AccountPosition pos : positions) {
			table.put(pos.getContract(), pos.getDirection(), pos);
		}
		return table;
	}

	@Test
	void testBasicProperties() {
		macc = new ModuleAccount("testModule", 100000, 10, 200, createPositionTable(), contractAccountMap);

		assertThat(macc.getId()).isEqualTo("testModule");
		assertThat(macc.accountBalance()).isEqualTo(99810); // 100000 + 10 - 200
		assertThat(macc.availableBalance()).isEqualTo(99810); // 无持仓占用
	}

	@Test
	void testAccountBalanceWithPositions() {
		AccountPosition longPos = new AccountPosition("testModule", contract, PositionDirectionEnum.PD_Long, repo);
		longPos.onTrade(openTrade);
		macc = new ModuleAccount("testModule", 100000, 10, 200, createPositionTable(longPos), contractAccountMap);

		// accountBalance = initBalance + initRealizedProfit - initCommission + realizedProfit()
		// realizedProfit = sum(accRealizedProfit) - sum(accCommission) = 0 - 0 = 0
		assertThat(macc.accountBalance()).isEqualTo(99810);

		// holdingMargin = 2 * 1000 * 10 * 0.08 = 1600
		assertThat(macc.availableBalance()).isEqualTo(98210); // 99810 - 1600
	}

	@Test
	void testGetPositionWithHolding() {
		AccountPosition longPos = new AccountPosition("testModule", contract, PositionDirectionEnum.PD_Long, repo);
		longPos.onTrade(openTrade);
		macc = new ModuleAccount("testModule", 100000, 0, 0, createPositionTable(longPos), contractAccountMap);

		Optional<Position> posOpt = macc.getPosition(PositionDirectionEnum.PD_Long, contract);
		assertThat(posOpt).isPresent();
		assertThat(posOpt.get().position()).isEqualTo(2);
	}

	@Test
	void testGetPositionEmpty() {
		macc = new ModuleAccount("testModule", 100000, 0, 0, createPositionTable(), contractAccountMap);

		Optional<Position> posOpt = macc.getPosition(PositionDirectionEnum.PD_Long, contract);
		assertThat(posOpt).isEmpty();
	}

	@Test
	void testSubmitOpenOrderSuccess() {
		macc = new ModuleAccount("testModule", 100000, 0, 0, createPositionTable(), contractAccountMap);

		SubmitOrderReq orderReq = SubmitOrderReq.builder()
				.contract(contract)
				.volume(1)
				.price(1000)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.build();

		macc.submitOrder(orderReq);
		verify(tradeAccount).submitOrder(orderReq);
	}

	@Test
	void testSubmitOpenOrderInsufficientBalance() {
		macc = new ModuleAccount("testModule", 1000, 0, 0, createPositionTable(), contractAccountMap);
		macc.setLogger(logger);
		// cost = 1000 * 10 * 10 * 0.08 = 8000 > 1000
		SubmitOrderReq orderReq = SubmitOrderReq.builder()
				.contract(contract)
				.volume(10)
				.price(1000)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.build();

		assertThatThrownBy(() -> macc.submitOrder(orderReq))
				.isInstanceOf(IllegalOrderRequestException.class);
	}

	@Test
	void testSubmitCloseOrderSuccess() {
		AccountPosition longPos = new AccountPosition("testModule", contract, PositionDirectionEnum.PD_Long, repo);
		longPos.onTrade(openTrade);
		macc = new ModuleAccount("testModule", 100000, 0, 0, createPositionTable(longPos), contractAccountMap);

		SubmitOrderReq orderReq = SubmitOrderReq.builder()
				.contract(contract)
				.volume(1)
				.price(1000)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.build();

		macc.submitOrder(orderReq);
		verify(tradeAccount).submitOrder(orderReq);
	}

	@Test
	void testSubmitCloseOrderInsufficientPosition() {
		AccountPosition longPos = new AccountPosition("testModule", contract, PositionDirectionEnum.PD_Long, repo);
		longPos.onTrade(openTrade);
		macc = new ModuleAccount("testModule", 100000, 0, 0, createPositionTable(longPos), contractAccountMap);
		macc.setLogger(logger);
		// 尝试平3手，但只有2手持仓
		SubmitOrderReq orderReq = SubmitOrderReq.builder()
				.contract(contract)
				.volume(3)
				.price(1000)
				.direction(DirectionEnum.D_Sell)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.build();

		assertThatThrownBy(() -> macc.submitOrder(orderReq))
				.isInstanceOf(IllegalOrderRequestException.class);
	}

	@Test
	void testSubmitOrderWithNoMappedAccount() {
		Contract otherContract = Contract.builder().unifiedSymbol("cu2205@SHFE@FUTURES").contractDefinition(cd).multiplier(10).build();
		macc = new ModuleAccount("testModule", 100000, 0, 0, createPositionTable(), contractAccountMap);

		SubmitOrderReq orderReq = SubmitOrderReq.builder()
				.contract(otherContract)
				.volume(1)
				.price(1000)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.build();

		assertThatThrownBy(() -> macc.submitOrder(orderReq))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("找不到对应的交易账户");
	}

	@Test
	void testCancelOrderSuccess() {
		macc = new ModuleAccount("testModule", 100000, 0, 0, createPositionTable(), contractAccountMap);

		CancelOrderReq cancelReq = CancelOrderReq.builder()
				.gatewayId("testAccount")
				.originOrderId("order123")
				.build();

		when(tradeAccount.cancelOrder(cancelReq)).thenReturn(true);

		boolean result = macc.cancelOrder(cancelReq);
		assertThat(result).isTrue();
		verify(tradeAccount).cancelOrder(cancelReq);
	}

	@Test
	void testCancelOrderAccountNotFound() {
		macc = new ModuleAccount("testModule", 100000, 0, 0, createPositionTable(), contractAccountMap);

		CancelOrderReq cancelReq = CancelOrderReq.builder()
				.gatewayId("nonExistentAccount")
				.originOrderId("order123")
				.build();

		assertThatThrownBy(() -> macc.cancelOrder(cancelReq))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("没有找到对应的账户");
	}

	@Test
	void testGetPendingOrdersThrowsException() {
		macc = new ModuleAccount("testModule", 100000, 0, 0, createPositionTable(), contractAccountMap);

		assertThatThrownBy(() -> macc.getPendingOrders())
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void testGetTradeGatewayThrowsException() {
		macc = new ModuleAccount("testModule", 100000, 0, 0, createPositionTable(), contractAccountMap);

		assertThatThrownBy(() -> macc.getTradeGateway())
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void testOnOrderDoesNothing() {
		macc = new ModuleAccount("testModule", 100000, 0, 0, createPositionTable(), contractAccountMap);

		Order order = Order.builder()
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.build();

		// 不应该抛出异常
		macc.onOrder(order);
	}

	@Test
	void testOnTradeDoesNothing() {
		macc = new ModuleAccount("testModule", 100000, 0, 0, createPositionTable(), contractAccountMap);

		Trade trade = Trade.builder()
				.contract(contract)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.price(1000)
				.volume(1)
				.tradeDate(LocalDate.now())
				.tradingDay(LocalDate.now())
				.tradeTime(LocalTime.now())
				.build();

		// 不应该抛出异常
		macc.onTrade(trade);
	}

}
