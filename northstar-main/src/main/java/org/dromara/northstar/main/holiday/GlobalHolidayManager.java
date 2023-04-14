package org.dromara.northstar.main.holiday;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tech.quantit.northstar.common.IHolidayManager;
import tech.quantit.northstar.common.constant.ChannelType;

/**
 * 
 * @author KevinHuangwl
 *
 */
@Component
public class GlobalHolidayManager implements InitializingBean{

	@Autowired
	private List<IHolidayManager> holidayMgr;
	
	private Map<ChannelType, IHolidayManager> holidayMgrMap;
	
	public boolean isHoliday(ChannelType gatewayType, LocalDateTime dateTime) {
		if(!holidayMgrMap.containsKey(gatewayType)) {
			return dateTime.toLocalDate().getDayOfWeek().getValue() > 5;	// 当没有找到对应的HolidayManager时，只判断周末是假日	
		}
		return holidayMgrMap.get(gatewayType).isHoliday(dateTime);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		holidayMgrMap = holidayMgr.stream()
				.collect(Collectors.toMap(IHolidayManager::channelType, mgr -> mgr));
	}
}
