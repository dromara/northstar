package org.dromara.northstar.common;

import java.time.LocalDateTime;

public interface IHolidayManager {

	boolean isHoliday(LocalDateTime dateTime);
	
}
