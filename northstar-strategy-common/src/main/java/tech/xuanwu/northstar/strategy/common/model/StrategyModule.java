package tech.xuanwu.northstar.strategy.common.model;

import java.util.List;

import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.ModuleAccount;
import tech.xuanwu.northstar.strategy.common.ModuleOrder;
import tech.xuanwu.northstar.strategy.common.ModulePosition;
import tech.xuanwu.northstar.strategy.common.ModuleTrade;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.model.data.BarData;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class StrategyModule {
	
	private BarData barData;
	
	private ModuleAccount mAccount;
	
	private ModulePosition mPosition;
	
	private ModuleOrder mOrder;
	
	private ModuleTrade mTrade;
	
	private SignalPolicy signalPolicy;
	
	private List<RiskControlRule> riskControlRuls;
	
	private Dealer dealer;
	
	private ModuleState state;
	
	public void onTick(TickField tick) {
		
	}
	
	public void onBar(BarField bar) {
		
	}
	
	public void onOrder(OrderField order) {
		
	}
	
	public void onTrade(TradeField trade) {
		
	}
	
	public void onAccount(AccountField account) {
		
	}
}
