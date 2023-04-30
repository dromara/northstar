package org.dromara.northstar.gateway.tiger.time;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;

import org.dromara.northstar.gateway.TradeTimeDefinition;
import org.dromara.northstar.gateway.common.domain.time.DateUtils;
import org.dromara.northstar.gateway.model.PeriodSegment;

/**
 * 美股连续交易时段
 * @author KevinHuangwl
 *
 */
public class UsStockTradeTime implements TradeTimeDefinition {
	

	@Override
	public List<PeriodSegment> tradeTimeSegments() {
		LocalDate now = LocalDate.now();
		LocalDate summerTimeStartDate = DateUtils.numOfWeekDay(now.getYear(), Month.MARCH, 2, DayOfWeek.SUNDAY);
		LocalDate summerTimeEndDate = DateUtils.numOfWeekDay(now.getYear(), Month.NOVEMBER, 1, DayOfWeek.SUNDAY);
		if(now.isAfter(summerTimeStartDate) && now.isBefore(summerTimeEndDate)) {			
			return List.of(new PeriodSegment(LocalTime.of(21, 1), LocalTime.of(4, 0)));
		}
		return List.of(new PeriodSegment(LocalTime.of(22, 1), LocalTime.of(5, 0)));
	}

}
