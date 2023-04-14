package org.dromara.northstar.common;

import java.time.LocalDateTime;

import org.dromara.northstar.common.constant.ChannelType;

public interface IHolidayManager {

	ChannelType channelType();
	
	boolean isHoliday(LocalDateTime dateTime);
}
