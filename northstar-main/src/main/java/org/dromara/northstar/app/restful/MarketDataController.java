package org.dromara.northstar.app.restful;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.dromara.northstar.app.service.MarketDataService;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.BarVO;
import org.dromara.northstar.common.model.TickVO;
import org.dromara.northstar.common.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("northstar/marketdata")
@RestController
public class MarketDataController {
	
	@Autowired
	MarketDataService service;
	
	/**
	 * 获取历史行情
	 * @param contractId
	 * @param barPeriod
	 * @param fromTime
	 * @param toTime
	 * @return
	 */
	@GetMapping("range/{contractId}/{interval}")
	public List<BarVO> getHistoricalBar(@PathVariable String contractId, @PathVariable String interval, 
			Long fromTime, Long toTime){
		Long to = Optional.ofNullable(toTime).orElse(System.currentTimeMillis());
		Long from = Optional.ofNullable(fromTime).orElse(CommonUtils.localDateTimeToMills(CommonUtils.millsToLocalDateTime(to).minusMonths(1)));
		Interval barInterval = Interval.fromAlias(interval);
		if(barInterval.getMinutes() > 60) {
			if(barInterval == Interval.WK1) {
				from = Optional.ofNullable(fromTime).orElse(CommonUtils.localDateTimeToMills(CommonUtils.millsToLocalDateTime(to).minusYears(2)));
			} else if (barInterval == Interval.D1) {
				from = Optional.ofNullable(fromTime).orElse(CommonUtils.localDateTimeToMills(CommonUtils.millsToLocalDateTime(to).minusYears(1)));
			} else {
				from = Optional.ofNullable(fromTime).orElse(CommonUtils.localDateTimeToMills(CommonUtils.millsToLocalDateTime(to).minusMonths(4)));
			}
		}
		return service.getRangeBars(contractId, barInterval, from, to);
	}
	
	/**
	 * 订阅BAR行情
	 * @param contractId
	 * @param barPeriod
	 */
	@GetMapping("subscribe")
	public void subscribe(String[] contractIds) {
		service.subscribe(Arrays.asList(contractIds));
	}
	
	/**
	 * 获取活跃行情
	 * @return
	 */
	@GetMapping("active")
	public List<TickVO> activeTicks(){
		return service.getActiveTicks();
	}
}
