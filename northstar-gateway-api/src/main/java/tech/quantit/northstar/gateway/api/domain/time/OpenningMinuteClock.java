package tech.quantit.northstar.gateway.api.domain.time;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 开市时钟
 * @author KevinHuangwl
 *
 */
public class OpenningMinuteClock {

	// 使用整点为分钟结束的交易所
	private static final Set<ExchangeEnum> exchangesOfEndByWholeMin = Set.of(ExchangeEnum.SHFE);

	private AtomicInteger cursor = new AtomicInteger();
	
	private List<LocalTime> timeFrame;
	
	private boolean endByWholeMin;
	
	private PeriodHelper helper;
	
	public OpenningMinuteClock(ContractField contract, PeriodHelperFactory phFactory) {
		helper = phFactory.newInstance(1, true, contract);
		timeFrame = helper.getRunningBaseTimeFrame();
		endByWholeMin = exchangesOfEndByWholeMin.contains(contract.getExchange());
	}
	
	/**
	 * 根据TICK时间计算所属的BAR时间
	 * 参考CTP接口数据为计算依据
	 * @param tick
	 * @return
	 */
	public LocalTime barMinute(TickField tick) {
		LocalTime tickTime = LocalTime.parse(tick.getActionTime(), DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER);
		LocalTime beginTimeOfTick = tickTime.withSecond(0).withNano(0);
		LocalTime endTimeOfTick = beginTimeOfTick.plusMinutes(1);
		if(endByWholeMin && beginTimeOfTick == tickTime) {
			updateCursor(tickTime);
			return tickTime;	// 整点为分钟收盘数据的情况
		}
		updateCursor(endTimeOfTick);
		return endTimeOfTick;
	}
	
	private void updateCursor(LocalTime t) {
		int index = timeFrame.indexOf(t);
		if(index < 0) {
			throw new IllegalStateException("找不到对应的时间：" + t);
		}
		cursor.set(index);
	}
	
	/**
	 * 下一个BAR时间
	 * @return
	 */
	public LocalTime nextBarMinute() {
		cursor.set(nextCursor());
		return timeFrame.get(cursor.get());
	}
	
	private int nextCursor() {
		return cursor.incrementAndGet() % timeFrame.size();
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
