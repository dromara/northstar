package org.dromara.northstar.common.model.core;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.gateway.Gateway;

import com.alibaba.fastjson.JSONObject;

import lombok.Builder;

@Builder
public record Tick(
		Gateway gateway,
		Contract contract,
		LocalDate actionDay,
		LocalTime actionTime,
		LocalDate tradingDay,
		double lastPrice,
		double avgPrice,
		double iopv,
		long volumeDelta,
		long volume,  			// 总成交量
		double turnover,  		// 成交总额
		double turnoverDelta,	// 成交总额变化
		double openInterest,  	// 持仓量
		double openInterestDelta,  	// 持仓量变化
		double settlePrice,  	// 结算价
		double preOpenInterest,		// 昨持仓
		double preClosePrice,  	// 前收盘价
		double preSettlePrice,  // 昨结算价
		double openPrice,  		// 开盘价
		double highPrice,  		// 最高价
		double lowPrice,  		// 最低价
		double upperLimit,  	// 涨停价
		double lowerLimit,  	// 跌停价
		List<Double> bidPrice,  	// 买价
		List<Double> askPrice,  	// 卖价
		List<Integer> bidVolume,  		// 买量
		List<Integer> askVolume,  		// 卖量
		TickType type,
		JSONObject otherInfo,	// 额外信息
		ChannelType channelType	// 渠道来源
	) {
	
}
