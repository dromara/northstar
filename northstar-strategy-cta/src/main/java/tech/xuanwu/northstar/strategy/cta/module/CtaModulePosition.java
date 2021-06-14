package tech.xuanwu.northstar.strategy.cta.module;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import tech.xuanwu.northstar.strategy.common.ModulePosition;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class CtaModulePosition implements ModulePosition{
	
	/**
	 * 暂存开仓成交,用于计算持仓盈亏,以及计算持仓状态
	 */
	private List<TradeField> openTrades = new ArrayList<>();
	
	public CtaModulePosition() {}
	
	public CtaModulePosition(List<TradeField> openTrades) {
		for(TradeField t : openTrades) {
			if(t.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
				this.openTrades.add(t);
			}
		}
	}

	@Override
	public List<TradeField> getOpenningTrade() {
		return openTrades;
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

	@Override
	public int getPositionProfit() {
		return 5260;
	}

}
