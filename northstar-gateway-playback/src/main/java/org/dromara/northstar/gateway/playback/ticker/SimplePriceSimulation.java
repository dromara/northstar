package org.dromara.northstar.gateway.playback.ticker;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.dromara.northstar.common.model.core.Bar;

public class SimplePriceSimulation implements TickSimulationAlgorithm {

	@Override
	public List<TickEntry> generateFrom(Bar bar) {
		double priceTick = bar.contract().priceTick();
		return List.of(
					randomAskBid(bar.openPrice(), priceTick, bar.volumeDelta() / 4, bar.openInterestDelta() / 4, bar.actionTimestamp() - 50000),
					randomAskBid(bar.highPrice(), priceTick, bar.volumeDelta() / 4, bar.openInterestDelta() / 4, bar.actionTimestamp() - 40000),
					randomAskBid(bar.lowPrice(), priceTick, bar.volumeDelta() / 4, bar.openInterestDelta() / 4, bar.actionTimestamp() - 30000),
					randomAskBid(bar.closePrice(), priceTick, bar.volumeDelta() / 4, bar.openInterestDelta() / 4, bar.actionTimestamp() - 20000)
				);
	}
	
	private TickEntry randomAskBid(double price, double priceTick, long volumeDelta, double openInterestDelta, long tickTime) {
		double askPrice = price + ThreadLocalRandom.current().nextInt(2) * priceTick;
		double bidPrice = askPrice - priceTick;
		return TickEntry.of(price, askPrice, bidPrice, volumeDelta, openInterestDelta, tickTime);
	}

}
