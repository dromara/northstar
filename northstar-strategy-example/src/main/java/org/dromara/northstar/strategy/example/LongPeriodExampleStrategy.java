package org.dromara.northstar.strategy.example;

import java.util.Set;

import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.constant.PeriodUnit;
import org.dromara.northstar.indicator.helper.SimpleValueIndicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;

/**
 * 大周期指标演示
 * @author KevinHuangwl
 *
 */
@StrategicComponent(LongPeriodExampleStrategy.NAME)
public class LongPeriodExampleStrategy extends AbstractStrategy implements TradeStrategy{

	protected static final String NAME = "示例-大周期指标演示";
	
	private InitParams params;	// 策略的参数配置信息
	
	private Set<Integer> validPeriod = Set.of(60,120);
	
	@Override
	protected void initIndicators() {
		if(!validPeriod.contains(ctx.numOfMinPerMergedBar())) {
			throw new IllegalArgumentException("只能设置60分钟或120分钟作为K线周期");
		}
		
		Contract c = ctx.getContract(params.indicatorSymbol);
		ctx.registerIndicator(new SimpleValueIndicator(Configuration.builder().indicatorName("C").contract(c).period(PeriodUnit.DAY).build()));
		ctx.registerIndicator(new SimpleValueIndicator(Configuration.builder().indicatorName("C").numOfUnits(2).contract(c).period(PeriodUnit.DAY).build()));
	}
	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		this.params = (InitParams) params;
	}
	
	public static class InitParams extends DynamicParams {			
		
		@Setting(label="指标合约", order=0)
		private String indicatorSymbol;
		
	}
}
