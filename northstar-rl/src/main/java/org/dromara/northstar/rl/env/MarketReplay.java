package org.dromara.northstar.rl.env;

import java.time.LocalDate;
import java.util.List;

import org.dromara.northstar.common.model.core.Bar;

/**
 * 行情回放游标：只做游标算术，永远不做 I/O、不知道任何 RL 概念。
 *
 * 数据在构造时一次性预载（Bar 数组，时间升序），之后所有 episode 的
 * {@link #locate} 只移动游标，零 I/O。
 *
 * 一个 episode 切片 = [episodeStart - warmupBars, episodeStart + length)，
 * 其中预热段只喂数据不采样、不计 dt、不计奖励；历史前缀不足时预热起点 clamp 到 0。
 */
public class MarketReplay {

	private final List<Bar> bars;	// 时间升序
	private int cursor = -1;
	private int warmupEndExclusive;
	private int endExclusive;

	public MarketReplay(List<Bar> bars) {
		if (bars.isEmpty()) {
			throw new IllegalArgumentException("回放数据不能为空");
		}
		this.bars = List.copyOf(bars);
		this.endExclusive = bars.size();
	}

	public int size() {
		return bars.size();
	}

	public Bar barAt(int idx) {
		return bars.get(idx);
	}

	public LocalDate dateAt(int idx) {
		return bars.get(idx).actionDay();
	}

	/** 第一个 actionDay >= date 的下标；全部早于 date 时返回 size() */
	public int lowerBound(LocalDate date) {
		int lo = 0, hi = bars.size();
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (bars.get(mid).actionDay().isBefore(date)) {
				lo = mid + 1;
			} else {
				hi = mid;
			}
		}
		return lo;
	}

	/**
	 * 定位一个 episode 切片。
	 *
	 * @param episodeStartIdx	episode 正式段起点下标
	 * @param episodeLength		正式段长度（bar 数）；超出数据末尾时截断
	 * @param warmupBars		预热前缀长度；历史前缀不足时 clamp
	 */
	public void locate(int episodeStartIdx, int episodeLength, int warmupBars) {
		if (episodeStartIdx < 0 || episodeStartIdx >= bars.size()) {
			throw new IllegalArgumentException("episode 起点越界: " + episodeStartIdx);
		}
		if (episodeLength <= 0) {
			throw new IllegalArgumentException("episode 长度必须为正数");
		}
		this.cursor = Math.max(0, episodeStartIdx - warmupBars) - 1;
		this.warmupEndExclusive = episodeStartIdx;
		this.endExclusive = Math.min(episodeStartIdx + episodeLength, bars.size());
	}

	/**
	 * 显式单步推进一根 bar。
	 *
	 * @return WARMUP 预热段 / OK 正式段 / EXHAUSTED 数据耗尽（游标不推进）
	 */
	public AdvanceResult advanceBar() {
		if (cursor + 1 >= endExclusive) {
			return AdvanceResult.EXHAUSTED;
		}
		cursor++;
		return cursor < warmupEndExclusive ? AdvanceResult.WARMUP : AdvanceResult.OK;
	}

	/** 当前 bar（advanceBar 返回 WARMUP/OK 后可用） */
	public Bar currentBar() {
		return bars.get(cursor);
	}

	public int cursor() {
		return cursor;
	}

	/** 切片内剩余可推进 bar 数 */
	public int barsRemaining() {
		return endExclusive - cursor - 1;
	}
}
