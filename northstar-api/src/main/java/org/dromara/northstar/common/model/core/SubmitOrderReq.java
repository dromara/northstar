package org.dromara.northstar.common.model.core;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;

@Builder(toBuilder = true)
public record SubmitOrderReq(
		String originOrderId,
		String accountCode,
		CurrencyEnum currency,
		Contract contract,
		String gatewayId,
		int volume,
		double price,
		OrderPriceTypeEnum orderPriceType,
		DirectionEnum direction,
		OffsetFlagEnum offsetFlag,
		HedgeFlagEnum hedgeFlag,
		TimeConditionEnum timeCondition,
		String gtdDate,
		VolumeConditionEnum volumeCondition,
		int minVolume,
		ContingentConditionEnum contingentCondition,
		double stopPrice,
		ForceCloseReasonEnum forceCloseReason,
		int autoSuspend,
		int userForceClose,
		int swapOrder,
		long actionTimestamp
		) {

}
