package tech.quantit.northstar.gateway.api.domain.time;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public class BarClock {

	private final List<LocalTime> baseTimeLine;
	
	private int cursor;
	
	public BarClock(List<LocalTime> baseTimeLine) {
		this.baseTimeLine = baseTimeLine;
	}
	
	/**
	 * 根据传入的时间，校正时间指针
	 * 
	 * @param time		
	 * @return			如果指针变化返回true，没变化返回false
	 */
	public boolean adjustTime(LocalTime time) {
		Optional<Duration> timeDiffOpt = baseTimeLine.stream()
				.map(t -> Duration.between(time, t))
				.filter(d -> !d.isNegative())
				.min((d1, d2) -> d1.compareTo(d2));
		if(timeDiffOpt.isPresent()) {
			LocalTime timeBucket = time.plus(timeDiffOpt.get());
			if(!timeBucket.equals(currentTimeBucket())) {
				cursor = baseTimeLine.indexOf(timeBucket);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 返回当前的时间槽
	 * @return
	 */
	public LocalTime currentTimeBucket() {
		return baseTimeLine.get(cursor);
	}
	
	/**
	 * 指针移到下一个时间槽
	 * @return
	 */
	public LocalTime next() {
		cursor = (cursor + 1) % baseTimeLine.size(); 
		return currentTimeBucket();
	}
	
	
}
