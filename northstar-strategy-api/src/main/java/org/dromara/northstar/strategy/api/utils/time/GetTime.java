package org.dromara.northstar.strategy.api.utils.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

public class GetTime {

	public static LocalTime from(TickField tick) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(tick.getActionTimestamp()), ZoneId.systemDefault()).toLocalTime();
	}
	
	public static LocalTime from(BarField bar) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(bar.getActionTimestamp()), ZoneId.systemDefault()).toLocalTime();
	}
}
