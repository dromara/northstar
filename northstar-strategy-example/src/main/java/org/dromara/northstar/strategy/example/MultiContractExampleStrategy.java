package org.dromara.northstar.strategy.example;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.trend.PBXIndicator;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;

/**
 * 本示例用于展示一个多周期多合约指标的策略
 * 
 * @author KevinHuangwl
 */
@StrategicComponent(MultiContractExampleStrategy.NAME)
public class MultiContractExampleStrategy extends AbstractStrategy implements TradeStrategy{

	protected static final String NAME = "示例-多合约多周期指标验证";	
	
	Contract c1;
	Contract c2;
	
	PBXIndicator pbx11;
	PBXIndicator pbx12;
	PBXIndicator pbx13;
	PBXIndicator pbx21;
	PBXIndicator pbx22;
	PBXIndicator pbx23;
	
	@Override
	protected void initIndicators() {
		ctx.bindedContracts().forEach(c -> {
			if(c1 == null) {
				c1 = c;
				pbx11 = new PBXIndicator(c1, Interval.M10);
				pbx12 = new PBXIndicator(c1, Interval.H1);
				pbx13 = new PBXIndicator(c1, Interval.D1);
				
			} else if(c2 == null) {
				c2 = c;
				pbx21 = new PBXIndicator(c2, Interval.M10);
				pbx22 = new PBXIndicator(c2, Interval.H1);
				pbx23 = new PBXIndicator(c2, Interval.D1);
			}
		});
		
		ctx.registerIndicator(pbx11);
		ctx.registerIndicator(pbx12);
		ctx.registerIndicator(pbx13);
		ctx.registerIndicator(pbx21);
		ctx.registerIndicator(pbx22);
		ctx.registerIndicator(pbx23);
			
	}

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
	}
	
	public static class InitParams extends DynamicParams {}

	@Override
	public String name() {
		return NAME;
	}

}
