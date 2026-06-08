package org.dromara.northstar.gateway.playback.model;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import org.dromara.northstar.common.Timed;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.CommonUtils;

import lombok.Builder;
import lombok.Data;

/**
 * 一份时间帧相同的数据
 * @auth KevinHuangwl
 */
public class DataFrame<T extends Timed> {

	private LinkedList<T> buf = new LinkedList<>();
	
	private final TimeEntry timeEntry;
	
	public DataFrame(long startTime, long endTime) {
		timeEntry = TimeEntry.builder()
				.start(CommonUtils.millsToLocalDateTime(startTime))
				.end(CommonUtils.millsToLocalDateTime(endTime))
				.build();
	}
	
	public DataFrame(LocalDateTime startTime, LocalDateTime endTime) {
		timeEntry = TimeEntry.builder()
				.start(startTime)
				.end(endTime)
				.build();
	}
	
	public synchronized void add(T item) {
		if(item instanceof Tick t && timeEntry.withinSection(t.actionTimestamp())
		|| item instanceof Bar b && timeEntry.withinSection(b.actionTimestamp())) {
			buf.add(item);
		}
	}
	
	public List<T> items(){
		return buf;
	}
	
	public TimeEntry getTime() {
		return timeEntry;
	}

	@Builder
	@Data
	public static class TimeEntry {
		
		private LocalDateTime start;
		
		private LocalDateTime end;
		
		public boolean withinSection(long time) {
			return time >= CommonUtils.localDateTimeToMills(start)
					&& time <= CommonUtils.localDateTimeToMills(end);
		}
	}
}
