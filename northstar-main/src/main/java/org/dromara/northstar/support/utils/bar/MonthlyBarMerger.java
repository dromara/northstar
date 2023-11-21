package org.dromara.northstar.support.utils.bar;

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
import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.model.PeriodSegment;

import xyz.redtorch.pb.CoreField.BarField;

/**
 * 月线合成器
 * @author KevinHuangwl
 *
 */
public class MonthlyBarMerger extends BarMerger{
	
	private final int numOfMonthPerBar;
	
	private Set<String> yMonthSet = new HashSet<>();
	
	private IDataSource dsMgr;
	
	/* year -> dateSet */
	private Map<Integer, Set<LocalDate>> yearHolidays = new HashMap<>();

	public MonthlyBarMerger(int numOfMonthPerBar, Contract contract, BiConsumer<BarMerger, BarField> callback, IDataSource dsMgr) {
		super(0, contract, callback);
		this.dsMgr = dsMgr;
		this.numOfMonthPerBar = numOfMonthPerBar;
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
		
		String yearMonth = toYearMonth(LocalDate.parse(bar.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER));
		yMonthSet.add(yearMonth);
		
		if(Objects.nonNull(barBuilder) && yMonthSet.size() == numOfMonthPerBar && isLastBarOfDay(bar) && isLastTradeDayOfMonth(getTradingDay(bar))) {
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
	
	private boolean isLastDayOfMonth(LocalDate tradingDay) {
		return tradingDay.plusDays(1).getMonth() != tradingDay.getMonth();
	}
	
	private boolean isLastTradeDayOfMonth(LocalDate date) {
		if(date.getDayOfMonth() < 26)
			return false;
		if(isLastDayOfMonth(date))
			return true;
		return isLastDayOfMonth(date.plusDays(1)) && isHoliday(date.plusDays(1));
	}

	private boolean isHoliday(LocalDate date) {
		int year = date.getYear();
		yearHolidays.computeIfAbsent(year, key -> holidaysOf(year));
		return yearHolidays.get(year).contains(date) || date.getDayOfWeek().getValue() > 5;
	}

	private boolean isLastBarOfDay(BarField bar) {
		List<PeriodSegment> segments = contract.tradeTimeDefinition().tradeTimeSegments();
		LocalTime time = segments.get(segments.size() - 1).endOfSegment();
		return LocalTime.parse(bar.getActionTime(), DateTimeConstant.T_FORMAT_FORMATTER).equals(time);
	}

	private LocalDate getTradingDay(BarField bar) {
		return LocalDate.parse(bar.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
	}

	@Override
	protected void doGenerate() {
		callback.accept(this, barBuilder.build());
		barBuilder = null;
		yMonthSet.clear();
	}

	private String toYearMonth(LocalDate date) {
		return String.format("%d%02d", date.getYear(), date.getMonthValue());
	}
}
