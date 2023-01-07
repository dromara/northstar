package tech.quantit.northstar.gateway.api.domain.time;

import java.time.LocalTime;
import java.util.List;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 开市时钟
 * @author KevinHuangwl
 *
 */
public class OpenningMinuteClock {

	private List<LocalTime> timeFrame;
	
	private PeriodHelper helper;
	
	private BarClock barClock;
	
	public OpenningMinuteClock(TradeTimeDefinition tradeTimeDefinition) {
		helper = new PeriodHelper(1, tradeTimeDefinition);
		timeFrame = helper.getRunningBaseTimeFrame();
		barClock = new BarClock(timeFrame);
	}
	
	/**
	 * 根据TICK时间计算所属的BAR时间
	 * 参考CTP接口数据为计算依据
	 * @param tick
	 * @return
	 */
	public LocalTime barMinute(TickField tick) {
		LocalTime tickTime = LocalTime.parse(tick.getActionTime(), DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER);
		barClock.adjustTime(tickTime);
		return barClock.currentTimeBucket();
	}
	
	/**
	 * 下一个BAR时间
	 * @return
	 */
	public LocalTime nextBarMinute() {
		return barClock.next();
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
