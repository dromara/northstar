package org.dromara.northstar.gateway.playback.ticker;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.dromara.northstar.common.model.core.Bar;

public class SimplePriceSimulation implements TickSimulationAlgorithm {

	private double priceTick;
	
	public SimplePriceSimulation(double priceTick) {
		this.priceTick = priceTick;
	}
	
	@Override
	public List<TickEntry> generateFrom(Bar bar) {
		return List.of(
					randomAskBid(bar.openPrice(), bar.volumeDelta() / 4, bar.openInterestDelta() / 4, bar.actionTimestamp() - 50000),
					randomAskBid(bar.highPrice(), bar.volumeDelta() / 4, bar.openInterestDelta() / 4, bar.actionTimestamp() - 40000),
					randomAskBid(bar.lowPrice(), bar.volumeDelta() / 4, bar.openInterestDelta() / 4, bar.actionTimestamp() - 30000),
					randomAskBid(bar.closePrice(), bar.volumeDelta() / 4, bar.openInterestDelta() / 4, bar.actionTimestamp() - 20000)
				);
	}
	
	private TickEntry randomAskBid(double price, long volumeDelta, double openInterestDelta, long tickTime) {
		double askPrice = price + ThreadLocalRandom.current().nextInt(2) * priceTick;
		double bidPrice = askPrice - priceTick;
		return TickEntry.of(price, askPrice, bidPrice, volumeDelta, openInterestDelta, tickTime);
	}

}
