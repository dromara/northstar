package tech.xuanwu.northstar.gateway.sim;

import java.util.Map;

import tech.xuanwu.northstar.engine.event.FastEventEngine;
import xyz.redtorch.pb.CoreField.ContractField;

public class SimFactory {
	
	private String gatewayId;
	private FastEventEngine feEngine;
	private int ticksOfCommission;
	private Map<String, ContractField> contractMap;
	
	public SimFactory(String gatewayId, FastEventEngine feEngine, int ticksOfCommission, Map<String, ContractField> contractMap) {
		this.gatewayId = gatewayId;
		this.feEngine = feEngine;
		this.ticksOfCommission = ticksOfCommission;
		this.contractMap = contractMap;
	}

	public GwAccountHolder newGwAccountHolder(SimGateway simGateway) {
		return new GwAccountHolder(gatewayId, feEngine, ticksOfCommission, this, simGateway);
	}
	
	public GwPositionHolder newGwPositionHolder() {
		return new GwPositionHolder(gatewayId, contractMap);
	}
	
	public GwOrderHolder newGwOrderHolder() {
		return new GwOrderHolder(gatewayId, ticksOfCommission, contractMap);
	}
	
}
