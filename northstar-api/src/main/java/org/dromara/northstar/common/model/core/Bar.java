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
		LocalDate actionDay,		// K线归属日
		LocalTime actionTime,		// K线归属时间
		LocalDate tradingDay,		// 交易日
		long actionTimestamp,		// K线归属时间戳
		double openPrice,			// 开盘价
		double highPrice,			// 最高价
		double lowPrice,			// 最低价
		double closePrice,			// 收盘价
		double openInterest,  		// 当天累计持仓量
		double openInterestDelta,  	// K线持仓量变化
		long volume,  				// 当天总成交量
		long volumeDelta,  			// K线成交量
		double turnover,  			// 当天成交总额
		double turnoverDelta,  		// K线成交总额
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
