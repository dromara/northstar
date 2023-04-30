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
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.TradeGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

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
		AccountField accountField = AccountField.newBuilder()
				.setBalance(1000.0)
				.build();

		tradeAccount.onAccount(accountField);
		assertEquals(1000.0, tradeAccount.accountBalance());
	}

	@Test
	void testAvailableAmount() {
		AccountField accountField = AccountField.newBuilder()
				.setAvailable(800)
				.build();

		tradeAccount.onAccount(accountField);
		assertEquals(800.0, tradeAccount.availableAmount());
	}

	@Test
	void testDegreeOfRisk() {
		AccountField accountField = AccountField.newBuilder()
				.setBalance(1000.0)
				.setAvailable(800)
				.build();

		tradeAccount.onAccount(accountField);
		assertThat(tradeAccount.degreeOfRisk()).isCloseTo(0.2, offset(1e-6));
	}

	@Test
	void testTryLockAmount_success() {
		AccountField accountField = AccountField.newBuilder()
				.setBalance(1000.0)
				.setAvailable(800)
				.build();

		tradeAccount.onAccount(accountField);

		Optional<UUID> lockId = tradeAccount.tryLockAmount(500.0);

		assertTrue(lockId.isPresent());
	}

	@Test
	void testTryLockAmount_failure() {
		AccountField accountField = AccountField.newBuilder()
				.setBalance(1000.0)
				.setAvailable(800)
				.build();

		tradeAccount.onAccount(accountField);

		Optional<UUID> lockId = tradeAccount.tryLockAmount(1000.0);

		assertFalse(lockId.isPresent());
	}

	@Test
	void testUnlockAmount() {
		AccountField accountField = AccountField.newBuilder()
				.setBalance(1000.0)
				.setAvailable(800)
				.build();

		tradeAccount.onAccount(accountField);

		UUID lockId = tradeAccount.tryLockAmount(800.0).orElseThrow();
		assertThat(tradeAccount.tryLockAmount(1)).isEmpty();
		tradeAccount.unlockAmount(lockId);
		
		assertThat(tradeAccount.tryLockAmount(1)).isPresent();
	}

	@Test
	void testSubmitOrder() {
		SubmitOrderReqField orderReq = SubmitOrderReqField.newBuilder()
				.setOriginOrderId("123")
				.build();
		when(tradeGateway.submitOrder(orderReq)).thenReturn("123");

		String orderId = tradeAccount.submitOrder(orderReq);

		assertEquals("123", orderId);
		verify(tradeGateway).submitOrder(orderReq);
	}

	@Test
	void testCancelOrder() {
		CancelOrderReqField cancelReq = CancelOrderReqField.newBuilder().build();
		when(tradeGateway.cancelOrder(cancelReq)).thenReturn(true);

		boolean result = tradeAccount.cancelOrder(cancelReq);

		assertTrue(result);
		verify(tradeGateway).cancelOrder(cancelReq);
	}

	// Add more tests for other methods in the TradeAccount class.
}