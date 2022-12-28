package tech.quantit.northstar.gateway.api.domain.time;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * K线周期分割器
 * @author KevinHuangwl
 *
 */
public class PeriodHelper {
	
	private static final LocalTime START_TIME = LocalTime.of(17, 0);
	
	private List<LocalTime> baseTimeFrame = new ArrayList<>(256); // 基准时间线
	private Set<LocalTime> endOfSections = new HashSet<>();
	
	public PeriodHelper(int numbersOfMinPerPeriod, TradeTimeDefinition tradeTimeDefinition) {
		this(numbersOfMinPerPeriod, tradeTimeDefinition, false);
	}
	
	public PeriodHelper(int numbersOfMinPerPeriod, TradeTimeDefinition tradeTimeDefinition, boolean exclusiveOpening) {
		List<PeriodSegment> tradeTimeSegments = tradeTimeDefinition.getPeriodSegments();
		LocalTime opening = tradeTimeSegments.get(0).startOfSegment();
		LocalTime ending = tradeTimeSegments.get(tradeTimeSegments.size() - 1).endOfSegment();
		LocalTime t = START_TIME.plusMinutes(1);
		int minCount = 0;
		while(t != START_TIME) {
			for(PeriodSegment ps : tradeTimeSegments) {
				endOfSections.add(ps.endOfSegment());
				while(ps.withinPeriod(t)) {
					if(t != opening && minCount == numbersOfMinPerPeriod || t == ending || t == opening && !exclusiveOpening) {
						baseTimeFrame.add(t);
						minCount = 1;
					} else {
						minCount++;
					}
					t = t.plusMinutes(1);
				}
			}
			t = t.plusMinutes(1);
		}
	}

	/**
	 * 获取K线时间基线
	 * @return
	 */
	public List<LocalTime> getRunningBaseTimeFrame(){
		return baseTimeFrame;
	}
	
	/**
	 * 当前时间是否要小节收盘
	 * @param t
	 * @return
	 */
	public boolean isEndOfSection(LocalTime t) {
		return endOfSections.contains(t);
	}
}
