package org.dromara.northstar.common.model.core;

import com.alibaba.fastjson2.annotation.JSONField;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.PositionField;

@Builder(toBuilder = true)
public record Position(
        String positionId,						// 仓位ID，通常是<合约代码@交易所代码@产品类型@方向@网关>
        PositionDirectionEnum positionDirection,	// 仓位方向
        double position,							// 持仓数量
        double frozen,								// 冻结数量
        double openPrice,						// 开仓均价
        double openPriceDiff,					// 开仓均价浮动盈亏
        double positionProfit,					// 持仓盈亏
        double positionProfitRatio,				// 持仓盈亏比例
        double useMargin,						// 占用保证金
        double exchangeMargin,					// 交易所保证金
        @JSONField(serialize = false)
        Contract contract,
        long updateTimestamp,
        @JSONField(serialize = false)
        Tick lastTick,
        String gatewayId
) {

	public PositionField toPositionField() {
		PositionField.Builder builder = PositionField.newBuilder();
		if(positionId != null) {
			builder.setPositionId(positionId);
		}
		if(gatewayId != null) {
			builder.setAccountId(gatewayId);
			builder.setGatewayId(gatewayId);
		}
		if (positionDirection != null) {
			builder.setPositionDirection(positionDirection);
		}
		if (contract != null) {
			builder.setContract(contract.toContractField());
		}
		if (lastTick != null) {
			builder.setLastPrice(lastTick.lastPrice());
		}
		return builder
				.setPosition(position)
				.setFrozen(frozen)
				.setOpenPrice(openPrice)
				.setOpenPriceDiff(openPriceDiff)
				.setPositionProfit(positionProfit)
				.setPositionProfitRatio(positionProfitRatio)
				.setUseMargin(useMargin)
				.build();
	}
}
