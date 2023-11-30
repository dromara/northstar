package org.dromara.northstar.common.model.core;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.PositionField;

@Builder
public record Position(
        String positionId,
        String accountId,
        PositionDirectionEnum positionDirection,
        int position,
        int frozen,
        int ydPosition,
        int ydFrozen,
        int tdPosition,
        int tdFrozen,
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

	public PositionField toPositionField() {
		return PositionField.newBuilder()
				.setPositionId(positionId)
				.setAccountId(accountId)
				.setPositionDirection(positionDirection)
				.setPosition(position)
				.setFrozen(frozen)
				.setYdPosition(ydPosition)
				.setYdFrozen(ydFrozen)
				.setTdPosition(tdPosition)
				.setTdFrozen(tdFrozen)
				.setLastPrice(lastPrice)
				.setPrice(price)
				.setPriceDiff(priceDiff)
				.setOpenPrice(openPrice)
				.setOpenPriceDiff(openPriceDiff)
				.setPositionProfit(positionProfit)
				.setPositionProfitRatio(positionProfitRatio)
				.setOpenPositionProfit(openPositionProfit)
				.setOpenPositionProfitRatio(openPositionProfitRatio)
				.setUseMargin(useMargin)
				.setExchangeMargin(exchangeMargin)
				.setContractValue(contractValue)
				.setHedgeFlag(hedgeFlag)
				.setContract(contract.toContractField())
				.setGatewayId(gatewayId)
				.build();
	}
}
