package tech.quantit.northstar.gateway.api.domain.time;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * K线周期分割器
 * @author KevinHuangwl
 *
 */
public class PeriodHelper {
	
	private static final LocalTime START_TIME = LocalTime.of(17, 0);
	
	private LinkedList<Set<LocalTime>> segmentQ = new LinkedList<>();
	private LinkedHashMap<LocalTime, Set<LocalTime>> periodSegmentsMap = new LinkedHashMap<>();
	private List<LocalTime> baseTimeFrame;
	private Set<LocalTime> endOfSections = new HashSet<>();
	
	public PeriodHelper(int numbersOfMinPerPeriod, TradeTimeDefinition tradeTimeDefinition) {
		this(numbersOfMinPerPeriod, tradeTimeDefinition, null);
	}
	
	public PeriodHelper(int numbersOfMinPerPeriod, TradeTimeDefinition tradeTimeDefinition, LocalTime inclusiveOpenningTime) {
		if(Objects.nonNull(inclusiveOpenningTime)) {
			periodSegmentsMap.put(inclusiveOpenningTime, new HashSet<>());
		}
		LocalTime t = START_TIME.plusMinutes(1);
		while(t != START_TIME) {
			boolean isTradeTime = false;
			for(PeriodSegment ps : tradeTimeDefinition.getPeriodSegments()) {
				endOfSections.add(ps.endOfSegment());
				if(ps.withinPeriod(t)) {
					isTradeTime = true;
					break;
				}
			}
			if(isTradeTime) {
				if(segmentQ.peekLast() == null || segmentQ.peekLast().size() == numbersOfMinPerPeriod) {
					segmentQ.offer(new HashSet<>());
				}
				segmentQ.peekLast().add(t);
				periodSegmentsMap.put(t, segmentQ.peekLast());
			}
			t = t.plusMinutes(1);
		}
		if(Objects.nonNull(inclusiveOpenningTime)) {
			segmentQ.get(0).add(inclusiveOpenningTime);
			periodSegmentsMap.get(inclusiveOpenningTime).addAll(segmentQ.get(0));
		}
		baseTimeFrame = periodSegmentsMap.keySet().stream().toList();
	}

	/**
	 * 是否处于同一K线周期内
	 * @param t1
	 * @param t2
	 * @return
	 */
	public boolean withinTheSamePeriod(LocalTime t1, LocalTime t2) {
		if(!periodSegmentsMap.containsKey(t1)) {
			return false;
		}
		return periodSegmentsMap.get(t1).contains(t2);
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
