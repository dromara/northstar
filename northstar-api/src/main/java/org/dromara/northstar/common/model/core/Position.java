package org.dromara.northstar.common.model.core;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

@Builder
public record Position(
        String positionId,
        String accountId,
        PositionDirectionEnum positionDirection,
        double position,
        double frozen,
        double ydPosition,
        double ydFrozen,
        double tdPosition,
        double tdFrozen,
        double lastPrice,
        double price,
        double priceDiff,
        double openPrice,
        double openPriceDiff,
        double positionProfit,
        double positionProfitRatio,
        double openPositionProfit,
        double openPositionProfitRatio,
        double useMargin,
        double exchangeMargin,
        double contractValue,
        HedgeFlagEnum hedgeFlag,
        Contract contract,
        String gatewayId
) {


}
