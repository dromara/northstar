package org.dromara.northstar.common.model.core;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.dromara.northstar.common.Timed;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.springframework.util.Assert;

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
		ChannelType channelType		// 渠道来源
	) implements Timed{

	public BarField toBarField() {
		return BarField.newBuilder()
				.setGatewayId(Optional.ofNullable(gatewayId).orElse(contract.gatewayId()))
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
	
	public static Bar of(BarField bar, Contract contract) {
		Assert.isTrue(contract.unifiedSymbol().equals(bar.getUnifiedSymbol()), () -> String.format("合约信息不一致：期望%s 实际%s", bar.getUnifiedSymbol(), contract.unifiedSymbol()));
		return Bar.builder()
				.gatewayId(bar.getGatewayId())
				.contract(contract)
				.actionDay(LocalDate.parse(bar.getActionDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.actionTime(LocalTime.parse(bar.getActionTime(), DateTimeConstant.T_FORMAT_FORMATTER))
				.tradingDay(LocalDate.parse(bar.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.actionTimestamp(bar.getActionTimestamp())
				.openPrice(bar.getOpenPrice())
				.highPrice(bar.getHighPrice())
				.lowPrice(bar.getLowPrice())
				.closePrice(bar.getClosePrice())
				.openInterest(bar.getOpenInterest())
				.openInterestDelta(bar.getOpenInterestDelta())
				.volume(bar.getVolume())
				.volumeDelta(bar.getVolumeDelta())
				.turnover(bar.getTurnover())
				.turnoverDelta(bar.getTurnoverDelta())
				.preOpenInterest(bar.getPreOpenInterest())
				.preClosePrice(bar.getPreClosePrice())
				.preSettlePrice(bar.getPreSettlePrice())
				.channelType(ChannelType.valueOf(bar.getChannelType()))
				.build();
	}

	@Override
	public long getTimestamp() {
		return actionTimestamp;
	}
}
