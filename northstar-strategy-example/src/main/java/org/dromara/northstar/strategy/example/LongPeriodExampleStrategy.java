package org.dromara.northstar.strategy.example;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.constant.ValueType;
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
	
	private Indicator c1;
	private Indicator h1;
	private Indicator l1;
	
	@Override
	protected void initIndicators() {
		if(Interval.H1 != ctx.moduleBarInterval() || Interval.H2 != ctx.moduleBarInterval()) {
			throw new IllegalArgumentException("只能设置60分钟或120分钟作为K线周期");
		}
		
		Contract c = ctx.getContract(params.indicatorSymbol);
		c1 = new SimpleValueIndicator(Configuration.builder().indicatorName("C").contract(c).interval(Interval.D1).ifPlotPerBar(true).build());
		h1 = new SimpleValueIndicator(Configuration.builder().indicatorName("H").contract(c).interval(Interval.D1).ifPlotPerBar(true).valueType(ValueType.HIGH).build());
		l1 = new SimpleValueIndicator(Configuration.builder().indicatorName("L").contract(c).interval(Interval.D1).ifPlotPerBar(true).valueType(ValueType.LOW).build());
		
		ctx.registerIndicator(c1);
		ctx.registerIndicator(h1);
		ctx.registerIndicator(l1);
	}
	
	@Override
	public void onMergedBar(Bar bar) {
		logger.info("{} {} 日线收盘价：{}，最高价：{}，最低价：{}", bar.actionDay(), bar.actionTime(), c1.value(0), h1.value(0), l1.value(0));
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

	@Override
	public String name() {
		return NAME;
	}
}
