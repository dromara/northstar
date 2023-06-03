package org.dromara.northstar.support.utils.bar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.IDataServiceManager;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.model.PeriodSegment;

import cn.hutool.core.date.LocalDateTimeUtil;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 周线合成器
 * @author KevinHuangwl
 *
 */
public class WeeklyBarMerger extends BarMerger{
	
	private final int numOfWeekPerBar;
	
	private Set<String> yearWeekSet = new HashSet<>();
	
	private IDataServiceManager dsMgr;
	/* year -> dateSet */
	private Map<Integer, Set<LocalDate>> yearHolidays = new HashMap<>();

	public WeeklyBarMerger(int numOfWeekPerBar, Contract contract, BiConsumer<BarMerger, BarField> callback, IDataServiceManager dsMgr) {
		super(0, contract, callback);
		this.dsMgr = dsMgr;
		this.numOfWeekPerBar = numOfWeekPerBar;
		int yearNow = LocalDate.now().getYear();
		yearHolidays.put(yearNow, holidaysOf(yearNow));
		yearHolidays.put(yearNow + 1, holidaysOf(yearNow + 1));
	}
	
	private Set<LocalDate> holidaysOf(int year){
		return dsMgr.getHolidays(contract.exchange(), LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
					.stream()
					.collect(Collectors.toSet());
	}

	@Override
	public void onBar(BarField bar) {
		if(!StringUtils.equals(bar.getUnifiedSymbol(), unifiedSymbol)) {
			return;
		}
		String ywk = toYearWeek(LocalDate.parse(bar.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER));
		yearWeekSet.add(ywk);

		if(Objects.nonNull(barBuilder) && yearWeekSet.size() == numOfWeekPerBar && isLastBarOfDay(bar) && isLastDayOfWeek(getTradingDay(bar))) {
			doMerge(bar);
			doGenerate();
			return;
		}
		
		if(Objects.isNull(barBuilder)) {
			barBuilder = bar.toBuilder();
			return;
		}
		
		doMerge(bar);
	}
	
	private LocalDate getTradingDay(BarField bar) {
		return LocalDate.parse(bar.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
	}
	
	private boolean isHoliday(LocalDate date) {
		int year = date.getYear();
		yearHolidays.computeIfAbsent(year, key -> holidaysOf(year));
		return yearHolidays.get(year).contains(date);
	}
	
	private boolean isLastDayOfWeek(LocalDate tradingDay) {
		return tradingDay.getDayOfWeek() == DayOfWeek.FRIDAY
			|| tradingDay.getDayOfWeek() == DayOfWeek.THURSDAY && isHoliday(tradingDay.plusDays(1))
			|| tradingDay.getDayOfWeek() == DayOfWeek.WEDNESDAY && isHoliday(tradingDay.plusDays(1)) && isHoliday(tradingDay.plusDays(2))
			|| tradingDay.getDayOfWeek() == DayOfWeek.TUESDAY && isHoliday(tradingDay.plusDays(1)) && isHoliday(tradingDay.plusDays(2)) && isHoliday(tradingDay.plusDays(3))
			|| tradingDay.getDayOfWeek() == DayOfWeek.MONDAY && isHoliday(tradingDay.plusDays(1)) && isHoliday(tradingDay.plusDays(2)) && isHoliday(tradingDay.plusDays(3)) && isHoliday(tradingDay.plusDays(4));
	}

	private boolean isLastBarOfDay(BarField bar) {
		List<PeriodSegment> segments = contract.tradeTimeDefinition().tradeTimeSegments();
		LocalTime time = segments.get(segments.size() - 1).endOfSegment();
		return LocalTime.parse(bar.getActionTime(), DateTimeConstant.T_FORMAT_FORMATTER).equals(time);
	}

	// 2022年第2周，表达为202202
	private String toYearWeek(LocalDate date) {
		return String.format("%d%02d", date.getYear(), LocalDateTimeUtil.weekOfYear(date));
	}
	
	@Override
	protected void doGenerate() {
		callback.accept(this, barBuilder.build());
		barBuilder = null;
		yearWeekSet.clear();
	}
}
