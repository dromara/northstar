package org.dromara.northstar.kline;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.common.utils.DateTimeUtils;

import lombok.AllArgsConstructor;
import lombok.Data;

public class BarTimePadding {

	/**
	 * 把分钟时间对齐，并保存一份映射表，以快速得到任意时刻的K线开盘时间与收盘时间
	 */
	protected final Map<MinuteEntry, BarTimeEntry> barTimeEntryMap = new LinkedHashMap<>();

	protected final List<MinuteEntry> entryTimeList = new ArrayList<>();

	protected final int barInterval;

	protected final Contract contract;

	public BarTimePadding(Interval interval, Contract contract) {
		this.barInterval = interval.getMinutes();
		this.contract = contract;
		genBarTimeMap();
	}

	// 该方法用于生成分钟线时间对齐的映射表
	// 先从合约定义中获取交易时间段，然后每分钟取整点分钟时间，作为key，对应的K线结算时间作为value
	// 例如：交易时间段为 09:00-11:30, 13:30-15:00
	// 注意由于bar时间取值不同，计得的cutoffTime会略有区别，
	// 例如若使用开始时间作为bar时间，每小段应该包含开始时间，例如K线时间为09:05的K线实际上是09:05-09:06的数据，cutoffTime应该为09:10
	// 若是使用结束时间作为bar时间，每小段应该包含结束时间，例如K线时间为09:05的K线实际上是09:04-09:05的数据，cutoffTime应该为09:05
	private void genBarTimeMap() {
		LocalDate dummyDate = LocalDate.now(); // 引入日期解决跨日计算问题
		LocalDateTime ldtStart = null;
		LocalDateTime ldtEnd = null;
		// 先根据交易时间段生成K线时间点列表
		for (TimeSlot tslot : contract.contractDefinition().tradeTimeDef().timeSlots()) {
			boolean overnight = !tslot.end().isAfter(tslot.start());
			ldtStart = LocalDateTime.of(dummyDate, tslot.start());
			ldtEnd = LocalDateTime.of(overnight ? dummyDate.plusDays(1) : dummyDate, tslot.end());
			LocalDateTime ldt = ldtStart;
			while (ldt.isBefore(ldtEnd)) {
				LocalTime startTime = DateTimeUtils.fromCacheTime(ldt.toLocalTime());
				LocalTime endTime = startTime.plusMinutes(1);
				entryTimeList.add(new MinuteEntry(startTime, endTime));
				ldt = ldt.plusMinutes(1);
			}
		}
		// 把分钟切分为段
		for (int i = 0; i < entryTimeList.size();) {
			int end = Math.min(i + barInterval, entryTimeList.size());
			List<MinuteEntry> section = entryTimeList.subList(i, end);
			if (section.isEmpty()) {
				break;
			}
			LocalTime openTime = section.getFirst().start;
			LocalTime cutoffTime = section.getLast().end;
			BarTimeEntry barTimeEntry = new BarTimeEntry(openTime, cutoffTime);
			section.forEach(me -> barTimeEntryMap.put(me, barTimeEntry));
			i += barInterval;
		}

	}

	private MinuteEntry entryOf(long time, boolean isBarTime) {
		LocalDateTime ldt = CommonUtils.millsToLocalDateTime(time);
		LocalTime localTime = ldt.toLocalTime();
		LocalTime startTime = localTime.withSecond(0).withNano(0);
		LocalTime endTime = startTime.plusMinutes(1);
		if (isBarTime && !contract.usingStartTimeInBarTime() && !isSectionStart(localTime) || isSectionEnd(localTime)) {
			endTime = localTime;
			startTime = endTime.minusMinutes(1);
		}
		return new MinuteEntry(startTime, endTime);
	}

	private boolean isSectionStart(LocalTime t) {
		List<TimeSlot> times = contract.contractDefinition().tradeTimeDef().timeSlots();
		for (TimeSlot ts : times) {
			if (ts.start().equals(t)) {
				return true;
			}
		}
		return false;
	}

	private boolean isSectionEnd(LocalTime t) {
		List<TimeSlot> times = contract.contractDefinition().tradeTimeDef().timeSlots();
		for (TimeSlot ts : times) {
			if (ts.end().equals(t)) {
				return true;
			}
		}
		return false;
	}

	private static final LocalTime TRADE_DATE_CUTOFF = LocalTime.of(20, 0);

	private LocalDateTime cutoffDateTime(MinuteEntry me, LocalDate actionDay, LocalTime actionTime) {
		if (!barTimeEntryMap.containsKey(me)) {
			throw new IllegalStateException(String.format("[%s, %s] 没有找到对应的cutoffTime", me.start, me.end));
		}
		LocalTime cutoffTime = barTimeEntryMap.get(me).getCloseTime();
		List<TimeSlot> times = contract.contractDefinition().tradeTimeDef().timeSlots();
		TimeSlot firstSection = times.getFirst();
		boolean isChnFuturesWithNightTrade = times.size() > 1 && firstSection.start().isAfter(TRADE_DATE_CUTOFF);
		if (isChnFuturesWithNightTrade && actionDay.getDayOfWeek() == DayOfWeek.SATURDAY
				&& !firstSection.withinSection(cutoffTime)) {
			actionDay = actionDay.plusDays(2);
		} else if (isChnFuturesWithNightTrade && actionDay.getDayOfWeek() == DayOfWeek.FRIDAY
				&& !actionTime.isBefore(firstSection.start()) && !firstSection.withinSection(cutoffTime)) {
			actionDay = actionDay.plusDays(3);
		} else if (cutoffTime.isBefore(actionTime)) {
			actionDay = actionDay.plusDays(1);
		}
		return LocalDateTime.of(actionDay, cutoffTime);
	}
	
	public boolean valid(Bar bar) {
		MinuteEntry entry = entryOf(bar.actionTimestamp(), true);
		return barTimeEntryMap.containsKey(entry);
	}

	public LocalDateTime cutoffTimeInMinute(Bar bar) {
		return cutoffDateTime(entryOf(bar.actionTimestamp(), true), bar.actionDay(), bar.actionTime());
	}
	
	public LocalDateTime cutoffTimeInMinute(Tick tick) {
		return cutoffDateTime(entryOf(tick.actionTimestamp(), false), tick.actionDay(), tick.actionTime());
	}
	
	public LocalDateTime cutoffTimeInMinute(long time) {
		LocalDateTime ldt = CommonUtils.millsToLocalDateTime(time);
		return cutoffDateTime(entryOf(time, true), ldt.toLocalDate(), ldt.toLocalTime());
	}

	/**
	 * 根据K线数据计算其对应的K线开盘时间
	 */
	public LocalDateTime openTimeInMinute(Bar bar) {
		return openDateTime(entryOf(bar.actionTimestamp(), true), bar.actionDay(), bar.actionTime());
	}

	/**
	 * 根据Tick数据计算其对应的K线开盘时间
	 */
	public LocalDateTime openTimeInMinute(Tick tick) {
		return openDateTime(entryOf(tick.actionTimestamp(), false), tick.actionDay(), tick.actionTime());
	}

	/**
	 * 根据时间戳计算其对应的K线开盘时间
	 */
	public LocalDateTime openTimeInMinute(long time) {
		LocalDateTime ldt = CommonUtils.millsToLocalDateTime(time);
		return openDateTime(entryOf(time, true), ldt.toLocalDate(), ldt.toLocalTime());
	}

	private LocalDateTime openDateTime(MinuteEntry me, LocalDate actionDay, LocalTime actionTime) {
		if (!barTimeEntryMap.containsKey(me)) {
			throw new IllegalStateException(String.format("[%s, %s] 没有找到对应的openTime", me.start, me.end));
		}
		LocalDateTime cutoffDt = cutoffDateTime(me, actionDay, actionTime);
		LocalTime openTime = barTimeEntryMap.get(me).getOpenTime();
		LocalDate openDay = cutoffDt.toLocalDate();
		// 当开盘时间（如23:00）晚于收盘时间（如00:00）时，说明K线跨越了午夜，开盘日期应为收盘日期的前一天
		if (openTime.isAfter(cutoffDt.toLocalTime())) {
			openDay = openDay.minusDays(1);
		}
		return LocalDateTime.of(openDay, openTime);
	}

	@AllArgsConstructor
	@Data
	protected class BarTimeEntry {

		private LocalTime openTime;

		private LocalTime closeTime;
	}

	@AllArgsConstructor
	@Data
	protected class MinuteEntry {

		private LocalTime start;

		private LocalTime end;
	}
}
