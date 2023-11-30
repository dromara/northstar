package org.dromara.northstar.common.model.core;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.TickType;

import com.alibaba.fastjson.JSONObject;

import lombok.Builder;
import xyz.redtorch.pb.CoreField.TickField;

@Builder
public record Tick(
		String gatewayId,
		Contract contract,
		LocalDate actionDay,
		LocalTime actionTime,
		LocalDate tradingDay,
		long actionTimestamp,
		double lastPrice,
		double avgPrice,
		double iopv,
		long volumeDelta,
		long volume,              // 总成交量
		double turnover,          // 成交总额
		double turnoverDelta,    // 成交总额变化
		double openInterest,      // 持仓量
		double openInterestDelta,      // 持仓量变化
		double settlePrice,      // 结算价
		double preOpenInterest,        // 昨持仓
		double preClosePrice,      // 前收盘价
		double preSettlePrice,  // 昨结算价
		double openPrice,          // 开盘价
		double highPrice,          // 最高价
		double lowPrice,          // 最低价
		double upperLimit,      // 涨停价
		double lowerLimit,      // 跌停价
		List<Double> bidPrice,      // 买价
		List<Double> askPrice,      // 卖价
		List<Integer> bidVolume,          // 买量
		List<Integer> askVolume,          // 卖量
		TickType type,
		JSONObject otherInfo,    // 额外信息
		ChannelType channelType    // 渠道来源
) {

	public TickField toTickField() {
		return TickField.newBuilder()
				.setGatewayId(gatewayId)
				.setUnifiedSymbol(contract.unifiedSymbol())
				.setActionDay(actionDay.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime(actionTime.format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER))
				.setTradingDay(tradingDay.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTimestamp(actionTimestamp)
				.setLastPrice(lastPrice)
				.setAvgPrice(avgPrice)
				.setIopv(iopv)
				.setVolumeDelta(volumeDelta)
				.setVolume(volume)
				.setTurnover(turnover)
				.setTurnoverDelta(turnoverDelta)
				.setOpenInterest(openInterest)
				.setOpenInterestDelta(openInterestDelta)
				.setSettlePrice(settlePrice)
				.setPreOpenInterest(preOpenInterest)
				.setPreClosePrice(preClosePrice)
				.setPreSettlePrice(preSettlePrice)
				.setOpenPrice(openPrice)
				.setHighPrice(highPrice)
				.setLowPrice(lowPrice)
				.setUpperLimit(upperLimit)
				.setLowerLimit(lowerLimit)
				.addAllBidPrice(bidPrice)
				.addAllAskPrice(askPrice)
				.addAllBidVolume(bidVolume)
				.addAllAskVolume(askVolume)
				.setChannelType(channelType.toString())
				.build();
	}
}
