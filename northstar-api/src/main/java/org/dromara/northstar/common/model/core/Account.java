package org.dromara.northstar.common.model.core;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreField.AccountField;

@Builder(toBuilder = true)
public record Account(
        CurrencyEnum currency,	// 币种
        double preBalance,		// 昨日账户结算净值
        double balance,			// 账户余额
        double available,		// 可用资金
        double commission,		// 今日手续费
        double margin,			// 保证金占用
        double realizedProfit,	// 平仓盈亏
        double unrealizedProfit,	// 持仓盈亏
        double deposit,			// 入金金额
        double withdraw,			// 出金金额
        String gatewayId
) {

	public AccountField toAccountField() {
		AccountField.Builder builder = AccountField.newBuilder();
		if(gatewayId != null) {
			builder.setGatewayId(gatewayId);
		}
		if (currency != null) {
			builder.setCurrency(currency);
		}
		return builder
				.setPreBalance(preBalance)
				.setBalance(balance)
				.setAvailable(available)
				.setCommission(commission)
				.setMargin(margin)
				.setCloseProfit(realizedProfit)
				.setPositionProfit(unrealizedProfit)
				.setDeposit(deposit)
				.setWithdraw(withdraw)
				.build();
	}

}
