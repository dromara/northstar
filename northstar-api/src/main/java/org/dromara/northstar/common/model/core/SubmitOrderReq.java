package org.dromara.northstar.common.model.core;

import com.alibaba.fastjson.JSONObject;
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
		String originOrderId,        // 本地订单ID
		CurrencyEnum currency,        // 币种
		Contract contract,            // 合约
		String gatewayId,
		int volume,                    // 下单数量
		double price,                // 下单价格
		OrderPriceTypeEnum orderPriceType,    // 定单价格类型
		DirectionEnum direction,    // 买卖方向
		OffsetFlagEnum offsetFlag,    // 开平标识
		TimeConditionEnum timeCondition,    // 时效
		String gtdDate,                // GTD日期
		int minVolume,                // 最小成交量
		ContingentConditionEnum contingentCondition,    // 触发条件
		double stopPrice,            // 止损价
		JSONObject extInfo,            // 扩展信息
		long actionTimestamp        // 操作时间戳
		) {

}
