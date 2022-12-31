package tech.quantit.northstar.common;

import java.time.LocalDateTime;

import tech.quantit.northstar.common.constant.ChannelType;

public interface IHolidayManager {

	ChannelType channelType();
	
	boolean isHoliday(LocalDateTime dateTime);
}
