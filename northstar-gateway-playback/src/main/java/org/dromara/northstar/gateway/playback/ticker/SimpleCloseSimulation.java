package org.dromara.northstar.gateway.playback.ticker;

import java.util.List;

import org.dromara.northstar.common.model.core.Bar;

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
	public List<TickEntry> generateFrom(Bar bar) {
		double askPrice = bar.closePrice() > bar.openPrice() ? bar.closePrice() : bar.closePrice() + priceTick;
		double bidPrice = bar.closePrice() > bar.openPrice() ? bar.closePrice() - priceTick : bar.closePrice();
		return List.of(TickEntry.of(bar.closePrice(), askPrice, bidPrice, bar.volumeDelta(), bar.openInterestDelta(), bar.actionTimestamp() - 30000));
	}
	
}
