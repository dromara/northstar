package org.dromara.northstar.common.model.core;

import java.time.LocalDate;
import java.time.LocalTime;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;

import com.alibaba.fastjson.JSONObject;

import lombok.Builder;
import xyz.redtorch.pb.CoreField.BarField;

@Builder(toBuilder = true)
public record Bar(
		String gatewayId,
		Contract contract,
		LocalDate actionDay,
		LocalTime actionTime,
		LocalDate tradingDay,
		long actionTimestamp,
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
		JSONObject otherInfo		// 额外信息
	) {

	public BarField toBarField() {
		return BarField.newBuilder()
				.setGatewayId(gatewayId)
				.setUnifiedSymbol(contract.unifiedSymbol())
				.setActionDay(actionDay.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime(actionTime.format(DateTimeConstant.T_FORMAT_FORMATTER))
				.setTradingDay(tradingDay.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTimestamp(actionTimestamp)
				.setOpenPrice(openPrice)
				.setHighPrice(highPrice)
				.setLowPrice(lowPrice)
				.setClosePrice(closePrice)
				.setOpenInterest(openInterest)
				.setOpenInterestDelta(openInterestDelta)
				.setVolume(volume)
				.setVolumeDelta(volumeDelta)
				.setTurnover(turnover)
				.setTurnoverDelta(turnoverDelta)
				.setPreOpenInterest(preOpenInterest)
				.setPreClosePrice(preClosePrice)
				.setPreSettlePrice(preSettlePrice)
				.setChannelType(channelType.toString())
				.build();
	}
}
