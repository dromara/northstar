package tech.xuanwu.northstar.gateway.sim.market;

import java.util.HashMap;
import java.util.Map;

import tech.xuanwu.northstar.common.utils.ContractNameResolver;
import xyz.redtorch.pb.CoreField.ContractField;

public class InstrumentBasePrice {
	
	private InstrumentBasePrice() {}

	private static Map<String, Double> priceMap = new HashMap<>() {

		private static final long serialVersionUID = 1L;
		{
			put("sim", 150000D);
		}
	};
	
	public static Double getBasePrice(ContractField contract) {
		String symbol = contract.getSymbol();
		String symbolGroup = ContractNameResolver.symbolToSymbolGroup(symbol);
		if(priceMap.containsKey(symbolGroup)) {			
			return priceMap.get(symbolGroup);
		}
		return 5000D;
	}
}
