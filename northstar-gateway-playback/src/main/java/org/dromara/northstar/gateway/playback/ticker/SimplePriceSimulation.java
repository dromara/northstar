package org.dromara.northstar.gateway.playback.ticker;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.utils.MarketDataUtils;

public class SimplePriceSimulation implements TickSimulationAlgorithm {

	@Override
	public List<TickEntry> generateFrom(Bar bar) {
		if(noTickSituation(bar)) {
			return Collections.emptyList();
		}
		int factor = bar.contract().usingStartTimeInBarTime() ? 1 : 0;
		double priceTick = bar.contract().priceTick();
		return List.of(
					randomAskBid(bar.openPrice(), priceTick, bar.volumeDelta() / 4, bar.openInterestDelta() / 4, bar.actionTimestamp() - 60000 + 60000 * factor),
					randomAskBid(bar.highPrice(), priceTick, bar.volumeDelta() / 4, bar.openInterestDelta() / 4, bar.actionTimestamp() - 45000 + 60000 * factor),
					randomAskBid(bar.lowPrice(), priceTick, bar.volumeDelta() / 4, bar.openInterestDelta() / 4, bar.actionTimestamp() - 30000 + 60000 * factor),
					randomAskBid(bar.closePrice(), priceTick, bar.volumeDelta() / 4, bar.openInterestDelta() / 4, bar.actionTimestamp() - 15000 + 60000 * factor)
				);
	}
	
	private boolean noTickSituation(Bar bar) {
		return !bar.contract().usingStartTimeInBarTime() && MarketDataUtils.isStartingBar(bar);
	}
	
	private TickEntry randomAskBid(double price, double priceTick, double volumeDelta, int openInterestDelta, long tickTime) {
		double askPrice = price + ThreadLocalRandom.current().nextInt(2) * priceTick;
		double bidPrice = askPrice - priceTick;
		return TickEntry.of(price, askPrice, bidPrice, volumeDelta, openInterestDelta, tickTime);
	}

}
