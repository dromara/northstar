package org.dromara.northstar.common.model.core;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;

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


}
