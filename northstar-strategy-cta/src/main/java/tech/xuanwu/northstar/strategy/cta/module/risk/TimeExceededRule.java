package tech.xuanwu.northstar.strategy.cta.module.risk;

import tech.xuanwu.northstar.strategy.common.DynamicParamsAware;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.constants.Signal;
import tech.xuanwu.northstar.strategy.common.model.DynamicParams;

@StrategicComponent("委托超时限制")
public class TimeExceededRule implements RiskControlRule, DynamicParamsAware{
	
	private long timeoutInterval;

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
		// TODO Auto-generated method stub
		
	}
	
	public class InitParams extends DynamicParams{
		
		@Label(value="超时时间", unit="秒")
		private int timeoutSeconds;
		
	}
}
