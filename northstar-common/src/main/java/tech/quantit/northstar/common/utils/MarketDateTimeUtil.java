package tech.quantit.northstar.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface MarketDateTimeUtil {

	
	public LocalDate getTradingDay(LocalDateTime dateTime);
}
