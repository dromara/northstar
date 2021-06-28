package tech.xuanwu.northstar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tech.xuanwu.northstar.common.constant.DateTimeConstant;

/**
 * 法定节假日管理器
 * @author KevinHuangwl
 *
 */
@Component
public class HolidayManager implements InitializingBean{

	@Value("${northstar.holidays}")
	protected String[] holidayStrs;
	
	protected Set<LocalDate> holidaySet = new HashSet<>();

	@Override
	public void afterPropertiesSet() throws Exception {
		for(String date : holidayStrs) {
			if(StringUtils.isEmpty(date)) {
				continue;
			}
			addHoliday(date);
		}
	}
	
	public boolean isHoliday(LocalDateTime dateTime) {
		LocalDate date = LocalDate.from(dateTime);
		boolean isWeekend = dateTime.getDayOfWeek().getValue() > 5;
		// 当天就是假期
		if(isWeekend || holidaySet.contains(date)) {
			return true;
		}
		// 当天不是假期的夜盘判断
		if(dateTime.getHour() >= 20) {
			boolean isFriday = dateTime.getDayOfWeek().getValue() == 5;
			date = LocalDate.from(dateTime.plusHours(isFriday ? 54 : 6));
		}
		if(dateTime.getHour() < 3) {
			date = LocalDate.from(dateTime.minusHours(4));
		}
		return holidaySet.contains(date) || date.getDayOfWeek().getValue() > 5;
	}
	
	public void addHoliday(String dateStr) {
		holidaySet.add(LocalDate.parse(dateStr, DateTimeConstant.D_FORMAT_INT_FORMATTER));
	}
}
