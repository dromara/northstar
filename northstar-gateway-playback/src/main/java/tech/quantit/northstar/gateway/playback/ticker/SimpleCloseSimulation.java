package tech.quantit.northstar.gateway.playback.ticker;

import java.util.List;

import xyz.redtorch.pb.CoreField.BarField;

/**
 * 收盘价生成策略
 * @author KevinHuangwl
 *
 */
public class SimpleCloseSimulation implements TickSimulationAlgorithm {
	
	private double priceTick;
	
	public SimpleCloseSimulation(double priceTick) {
		this.priceTick = priceTick;
	}
	
	@Override
	public List<TickEntry> generateFrom(BarField bar) {
		double askPrice = bar.getClosePrice() > bar.getOpenPrice() ? bar.getClosePrice() : bar.getClosePrice() + priceTick;
		double bidPrice = bar.getClosePrice() > bar.getOpenPrice() ? bar.getClosePrice() - priceTick : bar.getClosePrice();
		return List.of(TickEntry.of(bar.getClosePrice(), askPrice, bidPrice, bar.getVolume(), bar.getOpenInterestDelta(), bar.getActionTimestamp() - 30000));
	}
	
}
