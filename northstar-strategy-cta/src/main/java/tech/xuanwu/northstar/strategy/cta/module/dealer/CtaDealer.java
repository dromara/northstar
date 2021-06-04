package tech.xuanwu.northstar.strategy.cta.module.dealer;

import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.DynamicParamsAware;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.model.DynamicParams;

@StrategicComponent("CTA交易策略")
public class CtaDealer implements Dealer, DynamicParamsAware{
	
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
	
	public class InitParams extends DynamicParams{

		@Label(value="绑定合约")
		private String bindedUnifiedSymbol;
	}
}
