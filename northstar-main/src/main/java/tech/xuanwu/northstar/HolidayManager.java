package tech.xuanwu.northstar;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
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
	private String[] holidayStrs;
	
	private Set<LocalDate> holidaySet = new HashSet<>();

	@Override
	public void afterPropertiesSet() throws Exception {
		for(String date : holidayStrs) {
			if(StringUtils.isEmpty(date)) {
				continue;
			}
			holidaySet.add(LocalDate.parse(date, DateTimeConstant.D_FORMAT_INT_FORMATTER));
		}
	}
	
	public boolean isHoliday(LocalDate date) {
		return holidaySet.contains(date) || date.getDayOfWeek().getValue() > 5;
	}
}
