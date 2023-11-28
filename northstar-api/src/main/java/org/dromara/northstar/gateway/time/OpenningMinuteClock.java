package org.dromara.northstar.gateway.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.dromara.northstar.gateway.TradeTimeDefinition;

import xyz.redtorch.pb.CoreField.TickField;

/**
 * 开市时钟
 * @author KevinHuangwl
 *
 */
@Deprecated
public class OpenningMinuteClock {

	private List<LocalTime> timeFrame;
	
	private PeriodHelper helper;
	
	public OpenningMinuteClock(TradeTimeDefinition tradeTimeDefinition) {
		helper = new PeriodHelper(1, tradeTimeDefinition);
		timeFrame = helper.getRunningBaseTimeFrame();
	}
	
	/**
	 * 根据TICK时间计算所属的BAR时间
	 * @param tick
	 * @return
	 */
	public LocalDateTime barMinute(TickField tick) {
		LocalDateTime tickDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(tick.getActionTimestamp()), ZoneId.systemDefault());
		return tickDateTime.withSecond(0).withNano(0).plusMinutes(1);
	}
	
	/**
	 * 是否为合法的开市TICK
	 * @return
	 */
	public boolean isValidOpenningTick(TickField tick) {
		LocalDateTime ldt = barMinute(tick);
		LocalTime barTime = ldt.toLocalTime();
		return timeFrame.indexOf(barTime) >= 0;
	}
	
	/**
	 * 是否为小节末时间
	 * @param t
	 * @return
	 */
	public boolean isEndOfSection(LocalTime t) {
		return helper.isEndOfSection(t);
	}
	
}
