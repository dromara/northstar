package tech.xuanwu.northstar.gateway.sim;

import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.engine.event.FastEventEngine;

public class SimFactory {
	
	private String gatewayId;
	private FastEventEngine feEngine;
	private int ticksOfCommission;
	private ContractManager contractMgr;
	
	public SimFactory(String gatewayId, FastEventEngine feEngine, int ticksOfCommission, ContractManager contractMgr) {
		this.gatewayId = gatewayId;
		this.feEngine = feEngine;
		this.ticksOfCommission = ticksOfCommission;
		this.contractMgr = contractMgr;
	}

	public GwAccountHolder newGwAccountHolder() {
		return new GwAccountHolder(gatewayId, feEngine, ticksOfCommission, this);
	}
	
	public GwPositionHolder newGwPositionHolder() {
		return new GwPositionHolder(gatewayId, contractMgr);
	}
	
	public GwOrderHolder newGwOrderHolder() {
		return new GwOrderHolder(gatewayId, ticksOfCommission, contractMgr);
	}
	
}
