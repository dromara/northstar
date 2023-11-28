package org.dromara.northstar.common.model.core;

import java.time.LocalDate;
import java.time.LocalTime;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.gateway.Gateway;

import com.alibaba.fastjson.JSONObject;

import lombok.Builder;

@Builder
public record Bar(
		Gateway gateway,
		LocalDate actionDay,
		LocalTime actionTime,
		LocalDate tradingDay,
		double openPrice,
		double highPrice,
		double lowPrice,
		double closePrice,
		double openInterest,  		// 最后持仓量
		double openInterestDelta,  	// 持仓量（Bar）
		long volume,  				// 最后总成交量
		long volumeDelta,  			// 成交量（Bar）
		double turnover,  			// 最后成交总额
		double turnoverDelta,  		// 成交总额（Bar）
		double preOpenInterest,		// 昨持仓
		double preClosePrice,  		// 前收盘价
		double preSettlePrice,  	// 昨结算价
		ChannelType channelType,	// 渠道来源
		JSONObject otherInfo,		// 额外信息
		boolean unstable			// K线是否稳定
	) {

}
