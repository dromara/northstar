package tech.quantit.northstar.domain.module;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import cn.hutool.core.date.LocalDateTimeUtil;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 周线合成器
 * @author KevinHuangwl
 *
 */
public class WeeklyBarMerger extends BarMerger{

	public WeeklyBarMerger(int numOfMinPerBar, ContractField bindedContract, Consumer<BarField> callback) {
		super(0, bindedContract, callback);
	}

	@Override
	public void updateBar(BarField bar) {
		if(!StringUtils.equals(bar.getUnifiedSymbol(), bindedContract.getUnifiedSymbol())) {
			return;
		}
		
		if(Objects.nonNull(barBuilder) && !isSameWeek(toDate(bar.getTradingDay()), toDate(barBuilder.getTradingDay()))) {
			doGenerate();
		}
		
		if(Objects.isNull(barBuilder)) {
			barBuilder = bar.toBuilder();
			return;
		}
		
		double high = barBuilder.getHighPrice();
		double low = barBuilder.getLowPrice();
		long volumeDelta = barBuilder.getVolumeDelta();
		long numOfTradeDelta = barBuilder.getNumTradesDelta();
		double openInterestDelta = barBuilder.getOpenInterestDelta();
		double turnoverDelta = barBuilder.getTurnoverDelta();
		
		barBuilder
			.setActionDay(bar.getActionDay())
			.setActionTime(bar.getActionTime())
			.setActionTimestamp(bar.getActionTimestamp())
			.setTradingDay(bar.getTradingDay())
			.setClosePrice(bar.getClosePrice())
			.setHighPrice(Math.max(high, bar.getHighPrice()))
			.setLowPrice(Math.min(low, bar.getLowPrice()))
			.setVolume(bar.getVolume())
			.setVolumeDelta(volumeDelta + bar.getVolumeDelta())
			.setOpenInterest(bar.getOpenInterest())
			.setOpenInterestDelta(openInterestDelta + bar.getOpenInterestDelta())
			.setNumTrades(bar.getNumTrades())
			.setNumTradesDelta(numOfTradeDelta + bar.getNumTradesDelta())
			.setTurnover(bar.getTurnover())
			.setTurnoverDelta(turnoverDelta + bar.getTurnoverDelta());
	}
	
	private boolean isSameWeek(LocalDate date1, LocalDate date2) {
		return LocalDateTimeUtil.weekOfYear(date1) == LocalDateTimeUtil.weekOfYear(date2);
	}
	
	private LocalDate toDate(String dateStr) {
		return LocalDate.parse(dateStr, DateTimeConstant.D_FORMAT_INT_FORMATTER);
	}

}
