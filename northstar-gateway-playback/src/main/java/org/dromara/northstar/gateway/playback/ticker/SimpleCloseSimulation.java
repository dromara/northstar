package org.dromara.northstar.gateway.playback.ticker;

import java.util.Collections;
import java.util.List;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.utils.MarketDataUtils;

/**
 * 收盘价生成策略
 * @author KevinHuangwl
 *
 */
public class SimpleCloseSimulation implements TickSimulationAlgorithm {
	
	@Override
	public List<TickEntry> generateFrom(Bar bar) {
		if(!bar.contract().usingStartTimeInBarTime() && MarketDataUtils.isStartingBar(bar)) {
			// 自动跳过开盘第一个竞价TICK
			return Collections.emptyList();
		}
		// TICK时间戳不能取K线结束时刻（恰好等于整分钟时会被归入下一分钟K线），取分钟内的中间时刻
		int factor = bar.contract().usingStartTimeInBarTime() ? 1 : 0;
		long tickTime = bar.actionTimestamp() - 30000 + 60000 * factor;
		double priceTick = bar.contract().priceTick();
		double askPrice = bar.closePrice() > bar.openPrice() ? bar.closePrice() : bar.closePrice() + priceTick;
		double bidPrice = bar.closePrice() > bar.openPrice() ? bar.closePrice() - priceTick : bar.closePrice();
		return List.of(TickEntry.of(bar.closePrice(), askPrice, bidPrice, bar.volumeDelta(), bar.openInterestDelta(), tickTime));
	}
	
}
