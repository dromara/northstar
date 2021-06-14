package tech.xuanwu.northstar.strategy.cta.module;

import java.util.ArrayList;
import java.util.List;

import tech.xuanwu.northstar.strategy.common.ModuleTrade;
import tech.xuanwu.northstar.strategy.common.model.DealRecord;
import xyz.redtorch.pb.CoreField.TradeField;

public class CtaModuleTrade implements ModuleTrade{

	@Override
	public List<DealRecord> getDealRecords() {
		// TODO Auto-generated method stub
		return new ArrayList<>();
	}

	@Override
	public void updateTrade(TradeField trade) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getTotalCloseProfit() {
		// TODO Auto-generated method stub
		return 456123;
	}

}
