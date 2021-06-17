package tech.xuanwu.northstar.strategy.cta.module.dealer;

import java.util.List;
import java.util.Optional;

import tech.xuanwu.northstar.gateway.api.Gateway;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.model.OrderID;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

@StrategicComponent("CTA交易策略")
public class CtaDealer implements Dealer {
	
	private String bindedUnifiedSymbol;
	
	@Override
	public void onTick(TickField tick, List<RiskControlRule> riskRules, Gateway gateway) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Optional<OrderID> tryDeal(Signal signal, List<RiskControlRule> riskRules, Gateway gateway) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onOrder(OrderField order) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTrade(TradeField trade) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onAccount(AccountField account) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.bindedUnifiedSymbol = initParams.bindedUnifiedSymbol;
	}
	
	public static class InitParams extends DynamicParams{

		@Label(value="绑定合约")
		private String bindedUnifiedSymbol;
	}

	@Override
	public List<String> bindedUnifiedSymbols() {
		// TODO Auto-generated method stub
		return null;
	}

}
