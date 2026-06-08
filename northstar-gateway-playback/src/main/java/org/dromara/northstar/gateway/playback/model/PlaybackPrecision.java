package org.dromara.northstar.gateway.playback.model;

import org.dromara.northstar.gateway.playback.ticker.RandomWalkTickSimulation;
import org.dromara.northstar.gateway.playback.ticker.SimpleCloseSimulation;
import org.dromara.northstar.gateway.playback.ticker.SimplePriceSimulation;
import org.dromara.northstar.gateway.playback.ticker.TickSimulationAlgorithm;

/**
 * 回放精度
 * @author KevinHuangwl
 *
 */
public enum PlaybackPrecision {
	/**
	 * 每分钟1TICK
	 */
	LITE(1, new SimpleCloseSimulation()),	
	/**
	 * 每分钟4TICK
	 */
	LOW(4, new SimplePriceSimulation()),
	/**
	 * 每分钟30TICK
	 */
	MEDIUM(30, new RandomWalkTickSimulation(30)),
	/**
	 * 每分钟120TICK
	 */
	HIGH(120, new RandomWalkTickSimulation(120));
	
	private int numberOfTickPerBar;
	private TickSimulationAlgorithm algorithm;
	private PlaybackPrecision(int numberOfTickPerBar, TickSimulationAlgorithm algorithm) {
		this.numberOfTickPerBar = numberOfTickPerBar;
		this.algorithm = algorithm;
	}
	
	public int getNumberOfTickPerBar() {
		return numberOfTickPerBar;
	}
	
	public TickSimulationAlgorithm getAlgorithm() {
		return algorithm;
	}
}
