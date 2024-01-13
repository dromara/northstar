package org.dromara.northstar.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.TradeGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TradeAccountTest {

	private MarketGateway marketGateway;
	private TradeGateway tradeGateway;
	private GatewayDescription gatewayDescription;
	private TradeAccount tradeAccount;

	@BeforeEach
	void setUp() {
		marketGateway = mock(MarketGateway.class);
		tradeGateway = mock(TradeGateway.class);
		gatewayDescription = mock(GatewayDescription.class);
		tradeAccount = new TradeAccount(marketGateway, tradeGateway, gatewayDescription);
	}

	@Test
	void testAccountBalance() {
		Account account = Account.builder().balance(1000.0).build();
		tradeAccount.onAccount(account);
		assertEquals(1000.0, tradeAccount.accountBalance());
	}

	@Test
	void testAvailableAmount() {
		Account account = Account.builder().available(800).build();
		tradeAccount.onAccount(account);
		assertEquals(800.0, tradeAccount.availableAmount());
	}

	@Test
	void testDegreeOfRisk() {
		Account account = Account.builder().balance(1000.0).available(800).build();
		tradeAccount.onAccount(account);
		assertThat(tradeAccount.degreeOfRisk()).isCloseTo(0.2, offset(1e-6));
	}

	@Test
	void testTryLockAmount_success() {
		Account account = Account.builder().balance(1000.0).available(800).build();
		tradeAccount.onAccount(account);

		Optional<UUID> lockId = tradeAccount.tryLockAmount(500.0);

		assertTrue(lockId.isPresent());
	}

	@Test
	void testTryLockAmount_failure() {
		Account account = Account.builder().balance(1000.0).available(800).build();
		tradeAccount.onAccount(account);

		Optional<UUID> lockId = tradeAccount.tryLockAmount(1000.0);

		assertFalse(lockId.isPresent());
	}

	@Test
	void testUnlockAmount() {
		Account account = Account.builder().balance(1000.0).available(800).build();
		tradeAccount.onAccount(account);

		UUID lockId = tradeAccount.tryLockAmount(800.0).orElseThrow();
		assertThat(tradeAccount.tryLockAmount(1)).isEmpty();
		tradeAccount.unlockAmount(lockId);
		
		assertThat(tradeAccount.tryLockAmount(1)).isPresent();
	}

	@Test
	void testSubmitOrder() {
		SubmitOrderReq orderReq = SubmitOrderReq.builder()
				.originOrderId("123")
				.build();
		when(tradeGateway.submitOrder(orderReq)).thenReturn("123");

		String orderId = tradeAccount.submitOrder(orderReq);

		assertEquals("123", orderId);
		verify(tradeGateway).submitOrder(orderReq);
	}

	@Test
	void testCancelOrder() {
		when(tradeGateway.cancelOrder("orderId")).thenReturn(true);

		boolean result = tradeAccount.cancelOrder("orderId");

		assertTrue(result);
		verify(tradeGateway).cancelOrder("orderId");
	}

	// Add more tests for other methods in the TradeAccount class.
}