package tech.xuanwu.northstar.strategy.cta.module.dealer;

import java.util.List;

import tech.xuanwu.northstar.gateway.api.Gateway;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreField.TickField;

@StrategicComponent("CTA交易策略")
public class CtaDealer implements Dealer {
	
	private String bindedUnifiedSymbol;
	
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
	public void onTick(TickField tick, List<RiskControlRule> riskRules, Gateway gateway) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tryDeal(TickField tick, List<RiskControlRule> riskRules, Gateway gateway) {
		// TODO Auto-generated method stub
	}
}
