package tech.quantit.northstar.gateway.sim.market;

import java.util.concurrent.ThreadLocalRandom;

import xyz.redtorch.pb.CoreField.ContractField;

public class InstrumentBasePrice {
	
	private InstrumentBasePrice() {}

	public static Double getBasePrice(ContractField contract) {
		return 5000D + ThreadLocalRandom.current().nextDouble(-2000, 3000);
	}
}
