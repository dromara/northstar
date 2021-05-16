package tech.xuanwu.northstar.common.model;

import lombok.Data;

@Data
public class SimulationSettings implements GatewaySettings{
	
	/**
	 * 交易手续费
	 */
	private int ticksOfCommission;
}
