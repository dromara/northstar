package tech.quantit.northstar.main.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.IHolidayManager;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.data.IMarketDataRepository;

/**
 * 法定节假日管理器
 * @author KevinHuangwl
 *
 */
@Slf4j
@Component
public class CtpHolidayManager implements IHolidayManager, InitializingBean{

	protected Set<LocalDate> holidaySet = new HashSet<>();

	@Autowired
	private IMarketDataRepository repo;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		LocalDate today = LocalDate.now();
		holidaySet.addAll(repo.findHodidayInLaw(GatewayType.CTP, today.getYear()));
		holidaySet.addAll(repo.findHodidayInLaw(GatewayType.CTP, today.getYear() + 1));
		for(LocalDate date : holidaySet) {
			log.debug("假期日：{}", date);
		}
	}
	
	@Override
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
	
}
