package tech.xuanwu.northstar.gateway.sim.trade;

import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.common.event.FastEventEngine;

public class SimFactory {
	
	private String gatewayId;
	private FastEventEngine feEngine;
	private int fee;
	private ContractManager contractMgr;
	
	public SimFactory(String gatewayId, FastEventEngine feEngine, int fee, ContractManager contractMgr) {
		this.gatewayId = gatewayId;
		this.feEngine = feEngine;
		this.fee = fee;
		this.contractMgr = contractMgr;
	}

	public GwAccountHolder newGwAccountHolder() {
		return new GwAccountHolder(gatewayId, feEngine, fee, this);
	}
	
	public GwPositionHolder newGwPositionHolder() {
		return new GwPositionHolder(gatewayId, contractMgr);
	}
	
	public GwOrderHolder newGwOrderHolder() {
		return new GwOrderHolder(fee);
	}
	
}
