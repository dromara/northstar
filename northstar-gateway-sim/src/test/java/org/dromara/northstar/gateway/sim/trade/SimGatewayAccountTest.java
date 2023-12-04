package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

class SimGatewayAccountTest {

	SimGatewayAccount account = new SimGatewayAccount("testAccount");
	ContractDefinition cd = ContractDefinition.builder().commissionFee(5).build();
	Contract c1 = Contract.builder().symbol("rb2205").multiplier(10).longMarginRatio(0.08).shortMarginRatio(0.08).contractDefinition(cd).build();

	Trade openTrade = Trade.builder()
			.contract(c1).price(5000).volume(2).direction(DirectionEnum.D_Buy)
			.tradingDay(LocalDate.now().minusDays(1))
			.offsetFlag(OffsetFlagEnum.OF_Open).build();

	Trade closeTrade = Trade.builder()
			.contract(c1).price(5200).volume(2).direction(DirectionEnum.D_Sell)
			.tradingDay(LocalDate.now())
			.offsetFlag(OffsetFlagEnum.OF_Close).build();

	@BeforeEach
	void prepare() {
		OrderReqManager orderReqMgr = mock(OrderReqManager.class);
		when(orderReqMgr.totalFrozenAmount()).thenReturn(0D);
		account.setOrderReqMgr(orderReqMgr);
	}

	@Test
	void testAccountField() {
		assertThat(account.account()).isNotNull();
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
