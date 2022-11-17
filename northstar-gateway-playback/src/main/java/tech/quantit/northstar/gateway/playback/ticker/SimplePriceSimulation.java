package tech.quantit.northstar.gateway.playback.ticker;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import xyz.redtorch.pb.CoreField.BarField;

public class SimplePriceSimulation implements TickSimulationAlgorithm {

	private double priceTick;
	
	public SimplePriceSimulation(double priceTick) {
		this.priceTick = priceTick;
	}
	
	@Override
	public List<TickEntry> generateFrom(BarField bar) {
		return List.of(
					randomAskBid(bar.getOpenPrice(), bar.getVolume() / 4, bar.getOpenInterestDelta() / 4, bar.getActionTimestamp() - 50000),
					randomAskBid(bar.getHighPrice(), bar.getVolume() / 4, bar.getOpenInterestDelta() / 4, bar.getActionTimestamp() - 40000),
					randomAskBid(bar.getLowPrice(), bar.getVolume() / 4, bar.getOpenInterestDelta() / 4, bar.getActionTimestamp() - 30000),
					randomAskBid(bar.getClosePrice(), bar.getVolume() / 4, bar.getOpenInterestDelta() / 4, bar.getActionTimestamp() - 20000)
				);
	}
	
	private TickEntry randomAskBid(double price, long volume, double openInterestDelta, long tickTime) {
		double askPrice = price + ThreadLocalRandom.current().nextInt(2) * priceTick;
		double bidPrice = askPrice - priceTick;
		return TickEntry.of(price, askPrice, bidPrice, volume, openInterestDelta, tickTime);
	}

}
