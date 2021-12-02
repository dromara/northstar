package tech.xuanwu.northstar.domain.strategy;

import tech.xuanwu.northstar.strategy.api.MarketData;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

public class MarketDataHolder implements MarketData {
	
	private String unifiedSymbol;
	
	private MarketDataHolder(String unifiedSymbol) {
		this.unifiedSymbol = unifiedSymbol;
	}

	@Override
	public void onTick(TickField tick) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBar(BarField bar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String bindedContractSymbol() {
		return unifiedSymbol;
	}

}
