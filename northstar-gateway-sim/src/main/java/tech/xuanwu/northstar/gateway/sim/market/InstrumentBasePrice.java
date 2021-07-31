package tech.xuanwu.northstar.gateway.sim.market;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import tech.xuanwu.northstar.common.utils.ContractNameResolver;
import xyz.redtorch.pb.CoreField.ContractField;

public class InstrumentBasePrice {

	private static Map<String, Double> priceMap = new HashMap<>() {

		private static final long serialVersionUID = 1L;
		{
			put("ni", 144960D);
			put("rb", 5422D);
		}
	};
	
	public static Double getBasePrice(ContractField contract) {
		String symbol = contract.getSymbol();
		String symbolGroup = ContractNameResolver.symbolToSymbolGroup(symbol);
		return Optional.of(priceMap.get(symbolGroup)).get();
	}
}
