package tech.quantit.northstar.strategy.api.utils.time;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

import tech.quantit.northstar.strategy.api.utils.time.trade.TradeTimeDefinition;

/**
 * K线周期分割器
 * @author KevinHuangwl
 *
 */
public class PeriodHelper {
	
	private static final LocalTime START_TIME = LocalTime.of(17, 0);
	
	private LinkedList<Set<LocalTime>> segmentQ = new LinkedList<>();
	private LinkedHashMap<LocalTime, Set<LocalTime>> periodSegmentsMap = new LinkedHashMap<>();
	
	public PeriodHelper(int numbersOfMinPerPeriod, TradeTimeDefinition tradeTimeDefinition) {
		LocalTime t = START_TIME.plusMinutes(1);
		while(t != START_TIME) {
			boolean isTradeTime = false;
			for(PeriodSegment ps : tradeTimeDefinition.getPeriodSegments()) {
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
	}
	
	public PeriodHelper(int numbersOfMinPerPeriod, TradeTimeDefinition tradeTimeDefinition, LocalTime inclusiveOpenningTime) {
		this(numbersOfMinPerPeriod, tradeTimeDefinition);
		if(Objects.nonNull(inclusiveOpenningTime)) {
			segmentQ.get(0).add(inclusiveOpenningTime);
			periodSegmentsMap.put(inclusiveOpenningTime, segmentQ.get(0));
		}
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
}
