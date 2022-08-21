package tech.quantit.northstar.common;

import java.time.LocalDateTime;

public interface IHolidayManager {

	String gatewayType();
	
	boolean isHoliday(LocalDateTime dateTime);
}
