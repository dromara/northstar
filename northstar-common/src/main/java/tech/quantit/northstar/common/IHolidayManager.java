package tech.quantit.northstar.common;

import java.time.LocalDateTime;

public interface IHolidayManager {

	boolean isHoliday(LocalDateTime dateTime);
}
