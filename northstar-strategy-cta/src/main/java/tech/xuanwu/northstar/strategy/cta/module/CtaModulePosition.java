package tech.xuanwu.northstar.strategy.cta.module;

import java.time.Duration;
import java.util.List;

import tech.xuanwu.northstar.strategy.common.ModulePosition;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class CtaModulePosition implements ModulePosition{

	@Override
	public List<TradeField> getOpenningTrade() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Duration getPositionDuration() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNonfronzenVolume() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void onTick(TickField tick) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOrder(OrderField order) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTrade(TradeField order) {
		// TODO Auto-generated method stub
		
	}

}
