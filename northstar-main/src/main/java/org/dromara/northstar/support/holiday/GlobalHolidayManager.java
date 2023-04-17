package org.dromara.northstar.support.holiday;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dromara.northstar.common.IHolidayManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
