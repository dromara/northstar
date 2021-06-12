package tech.xuanwu.northstar.strategy.cta.module.risk;

import tech.xuanwu.northstar.strategy.common.DynamicParamsAware;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.model.CtaSignal;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;

@StrategicComponent("委托超价限制")
public class PriceExceededRule implements RiskControlRule, DynamicParamsAware{

	private int priceDifTolleranceInTick;
	
	@Override
	public boolean canDeal(CtaSignal ctaSignal) {
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
		this.priceDifTolleranceInTick = initParams.priceDifTolleranceInTick;
	}
	
	public class InitParams extends DynamicParams{
		
		@Label(value="超价限制", unit="Tick")
		private int priceDifTolleranceInTick;
		
	}
}
