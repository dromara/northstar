package tech.xuanwu.northstar.strategy.cta.module.risk;

import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;

@StrategicComponent("日内开仓次数限制")
public class DailyDealLimitedRule implements RiskControlRule {
	
	private int dailyDealLimit;

	@Override
	public boolean canDeal(Signal signal) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.dailyDealLimit = initParams.dailyDealLimit;
	}
	
	public class InitParams extends DynamicParams{
		
		@Label(value="日内开仓限制", unit="次")
		private int dailyDealLimit;
		
	}

	
}
