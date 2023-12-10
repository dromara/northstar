package org.dromara.northstar.common.model.core;

import com.alibaba.fastjson.annotation.JSONField;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;

@Builder(toBuilder = true)
public record SubmitOrderReq(
		String originOrderId,        // 本地订单ID
		CurrencyEnum currency,        // 币种
		@JSONField(serialize = false)
		Contract contract,            // 合约
		String gatewayId,
		int volume,                    // 下单数量
		double price,                // 下单价格
		OrderPriceTypeEnum orderPriceType,    // 定单价格类型
		DirectionEnum direction,    // 买卖方向
		OffsetFlagEnum offsetFlag,    // 开平标识
		TimeConditionEnum timeCondition,    // 时效
		VolumeConditionEnum volumeCondition, //成交条件
		String gtdDate,                // GTD日期
		int minVolume,                // 最小成交量
		ContingentConditionEnum contingentCondition,    // 触发条件
		double stopPrice,            // 止损价
		long actionTimestamp        // 操作时间戳
		) {
	
	@Override
	public String toString() {
		String unifiedSymbol = null;
		if(contract != null) {
			unifiedSymbol = contract.unifiedSymbol();
		}
		return "SubmitOrderReq [originOrderId=" + originOrderId + ", currency=" + currency + ", contract=" + unifiedSymbol
				+ ", gatewayId=" + gatewayId + ", volume=" + volume + ", price=" + price + ", orderPriceType="
				+ orderPriceType + ", direction=" + direction + ", offsetFlag=" + offsetFlag + ", timeCondition="
				+ timeCondition + ", volumeCondition=" + volumeCondition + ", gtdDate=" + gtdDate + ", minVolume="
				+ minVolume + ", contingentCondition=" + contingentCondition + ", stopPrice=" + stopPrice
				+ ", actionTimestamp=" + actionTimestamp + "]";
	}
	
}
