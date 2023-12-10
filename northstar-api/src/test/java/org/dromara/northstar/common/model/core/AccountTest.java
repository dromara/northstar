package org.dromara.northstar.common.model.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import xyz.redtorch.pb.CoreEnum;
import xyz.redtorch.pb.CoreField;

class AccountTest {

	@Test
	void testToAccountField() {
		Account account = Account.builder()
				.accountId("accountId")
				.available(1.0)
				.balance(2.0)
				.closeProfit(3.0)
				.commission(4.0)
				.currency(CoreEnum.CurrencyEnum.CNY)
				.deposit(5.0)
				.gatewayId("gatewayId")
				.margin(6.0)
				.positionProfit(7.0)
				.preBalance(8.0)
				.withdraw(9.0)
				.build();
		CoreField.AccountField accountField = account.toAccountField();
		assertEquals("accountId", accountField.getAccountId());
		assertEquals(1.0, accountField.getAvailable());
		assertEquals(2.0, accountField.getBalance());
		assertEquals(3.0, accountField.getCloseProfit());
		assertEquals(4.0, accountField.getCommission());
		assertEquals(CoreEnum.CurrencyEnum.CNY, accountField.getCurrency());
		assertEquals(5.0, accountField.getDeposit());
		assertEquals("gatewayId", accountField.getGatewayId());
		assertEquals(6.0, accountField.getMargin());
		assertEquals(7.0, accountField.getPositionProfit());
		assertEquals(8.0, accountField.getPreBalance());
		assertEquals(9.0, accountField.getWithdraw());
	}

}
