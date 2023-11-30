package org.dromara.northstar.common.model.core;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreField;
import xyz.redtorch.pb.CoreField.AccountField;

@Builder
public record Account(
        String accountId,
        String code,
        String name,
        String holder,
        CurrencyEnum currency,
        double preBalance,
        double balance,
        double available,
        double commission,
        double margin,
        double closeProfit,
        double positionProfit,
        double deposit,
        double withdraw,
        String gatewayId
) {

	public AccountField toAccountField() {
		return CoreField.AccountField.newBuilder()
				.setAccountId(accountId)
				.setCode(code)
				.setName(name)
				.setHolder(holder)
				.setCurrency(currency)
				.setPreBalance(preBalance)
				.setBalance(balance)
				.setAvailable(available)
				.setCommission(commission)
				.setMargin(margin)
				.setCloseProfit(closeProfit)
				.setPositionProfit(positionProfit)
				.setDeposit(deposit)
				.setWithdraw(withdraw)
				.setGatewayId(gatewayId)
				.build();
	}
}
