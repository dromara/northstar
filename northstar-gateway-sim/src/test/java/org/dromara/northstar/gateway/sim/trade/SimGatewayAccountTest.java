package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.TradeField;

class SimGatewayAccountTest {
	
	SimGatewayAccount account = new SimGatewayAccount("testAccount");

	TestFieldFactory factory = new TestFieldFactory("testGateway");
	TradeField openTrade = factory.makeTradeField("rb2205", 5000, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, "20220404");
	TradeField closeTrade = factory.makeTradeField("rb2205", 5200, 2, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close);
	
	@BeforeEach
	void prepare() {
		OrderReqManager orderReqMgr = mock(OrderReqManager.class);
		when(orderReqMgr.totalFrozenAmount()).thenReturn(0D);
		account.setOrderReqMgr(orderReqMgr);
	}
	
	@Test
	void testAccountField() {
		assertThat(account.accountField()).isNotNull();
	}

	@Test
	void testBalance() {
		account.onDeposit(100000);
		account.onWithdraw(10000);
		assertThat(account.balance()).isCloseTo(90000, offset(1e-4));
	}

	@Test
	void testAvailable() {
		account.onDeposit(100000);
		account.onWithdraw(10000);
		assertThat(account.available()).isCloseTo(90000, offset(1e-4));
	}

	@Test
	void testGetAccountDescription() {
		assertThat(account.getAccountDescription()).isNotNull();
	}

	@Test
	void testOnTrade() {
		account.onTrade(openTrade);
		
		assertThat(account.balance()).isCloseTo(-10, offset(1e-4));
	}

	@Test
	void testOnDeal() {
		Deal deal = Deal.builder().openTrade(openTrade).closeTrade(closeTrade).build();
		account.onDeal(deal);
		assertThat(account.balance()).isCloseTo(4000, offset(1e-4));
	}

	@Test
	void testOnDeposit() {
		account.onDeposit(10);
		
		assertThat(account.balance()).isCloseTo(10, offset(1e-4));
	}

	@Test
	void testOnWithdraw() {
		account.onWithdraw(10);
		
		assertThat(account.balance()).isCloseTo(-10, offset(1e-4));
	}

}
