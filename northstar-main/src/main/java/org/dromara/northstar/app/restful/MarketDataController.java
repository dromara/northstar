package org.dromara.northstar.app.restful;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.dromara.northstar.app.service.MarketDataService;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.BarVO;
import org.dromara.northstar.common.model.TickVO;
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
	 * @param interval
	 * @param fromTime
	 * @param toTime
	 * @return
	 */
	@GetMapping("range/{contractId}/{interval}")
	public List<BarVO> getHistoricalBar(@PathVariable String contractId, @PathVariable String interval,  Long fromTime, Long toTime){
		Interval barInterval = Interval.fromAlias(interval);
		
		long end = Optional.ofNullable(toTime).orElse(System.currentTimeMillis());
		long start = Optional.ofNullable(fromTime).orElse(end - Math.max(TimeUnit.DAYS.toMillis(1), TimeUnit.MINUTES.toMillis(barInterval.getMinutes() * 300L)));
		
		return service.getRangeBars(contractId, barInterval, start, end)
				.stream()
				.map(bar -> BarVO.builder()
						.timestamp(bar.actionTimestamp())
						.open(bar.openPrice())
						.close(bar.closePrice())
						.high(bar.highPrice())
						.low(bar.lowPrice())
						.volume(bar.volumeDelta())
						.oi(bar.openInterest())
						.tradingDay(bar.tradingDay().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
						.interval(interval)
						.build())
				.toList();
	}
	
	/**
	 * 订阅BAR行情
	 * @param contractId
	 * @param interval
	 */
	@GetMapping("subscribe")
	public void subscribe(String[] contractIds, String[] intervals) {
		service.subscribe(Arrays.asList(contractIds), Arrays.asList(intervals).stream().map(Interval::fromAlias).toList());
	}
	
	/**
	 * 获取活跃行情
	 * @return
	 */
	@GetMapping("active")
	public List<TickVO> activeTicks(){
		return service.getActiveTicks()
				.stream()
				.map(tick -> TickVO.builder()
						.name(tick.contract().name())
						.symbol(tick.contract().symbol())
						.lastPrice(tick.lastPrice())
						.priceChange(tick.lastPrice() - tick.preSettlePrice())
						.openInterest(tick.openInterest())
						.openInterestDelta(tick.openInterestDelta())
						.volume(tick.volume())
						.timestamp(tick.actionTimestamp())
						.precision(tick.contract().pricePrecision())
						.channel(tick.channelType())
						.build())
				.toList();
	}
}
