package tech.quantit.northstar.gateway.sim.market;

import xyz.redtorch.pb.CoreField.ContractField;

public class InstrumentBasePrice {
	
	private InstrumentBasePrice() {}

	public static Double getBasePrice(ContractField contract) {
		return 5000D;
	}
}
