package org.dromara.northstar.strategy.example;

import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.constant.PeriodUnit;
import org.dromara.northstar.indicator.model.Configuration;
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
				pbx11 = new PBXIndicator(Configuration.builder()
						.contract(c1)
						.numOfUnits(10)
						.period(PeriodUnit.MINUTE)
						.indicatorName("PBX1-1")
						.build());
				pbx12 = new PBXIndicator(Configuration.builder()
						.contract(c1)
						.numOfUnits(1)
						.period(PeriodUnit.HOUR)
						.indicatorName("PBX1-2")
						.build());
				pbx13 = new PBXIndicator(Configuration.builder()
						.contract(c1)
						.numOfUnits(1)
						.period(PeriodUnit.DAY)
						.indicatorName("PBX1-3")
						.build());
				
			} else if(c2 == null) {
				c2 = c;
				pbx21 = new PBXIndicator(Configuration.builder()
						.contract(c2)
						.numOfUnits(10)
						.period(PeriodUnit.MINUTE)
						.indicatorName("PBX2-1")
						.build());
				pbx22 = new PBXIndicator(Configuration.builder()
						.contract(c2)
						.numOfUnits(1)
						.period(PeriodUnit.HOUR)
						.indicatorName("PBX2-2")
						.build());
				pbx23 = new PBXIndicator(Configuration.builder()
						.contract(c2)
						.numOfUnits(1)
						.period(PeriodUnit.DAY)
						.indicatorName("PBX2-3")
						.build());
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
