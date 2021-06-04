package tech.xuanwu.northstar.strategy.cta.module.risk;

import tech.xuanwu.northstar.strategy.common.DynamicParamsAware;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.constants.Signal;
import tech.xuanwu.northstar.strategy.common.model.DynamicParams;

/**
 * 
 * @author KevinHuangwl
 *
 */
@StrategicComponent("保证金额度限制")
public class UseMarginExceededRule implements RiskControlRule, DynamicParamsAware{

	private double limitedPercentageOfTotalBalance;

	@Override
	public boolean canDeal(Signal signal) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DynamicParams getDynamicParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.limitedPercentageOfTotalBalance = initParams.limitedPercentageOfTotalBalance / 100.0;
	}
	
	
	public class InitParams extends DynamicParams{
		
		@Label(value="保证金上限", unit="%")
		private double limitedPercentageOfTotalBalance;
		
	}
}
