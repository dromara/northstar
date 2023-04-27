package org.dromara.northstar.strategy.example;

import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.indicator.Configuration;
import org.dromara.northstar.indicator.trend.EMAIndicator;
import org.dromara.northstar.indicator.trend.MAIndicator;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;

import com.google.common.util.concurrent.AtomicDouble;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 本策略没有交易逻辑，仅用于做指标演示
 * @author KevinHuangwl
 *
 */
@StrategicComponent(IndicatorDemoStrategy.NAME)
public class IndicatorDemoStrategy extends AbstractStrategy	// 为了简化代码，引入一个通用的基础抽象类
	implements TradeStrategy{

	protected static final String NAME = "示例-指标演示";
	
	private InitParams params;	// 策略的参数配置信息
	
	private final AtomicDouble valueHolder = new AtomicDouble();
	
	@Override
	public void onMergedBar(BarField bar) {
		// 当夜盘时值为0，日盘时值为1
		valueHolder.set(bar.getActionDay().equals(bar.getTradingDay()) ? 1 : 0);
	}

	@Override
	protected void initIndicators() {
		//######## 以下写法仅用于监控台演示，因此没有赋值给类属性，同时为了简化参数也直接写死 ########//
		ctx.registerIndicator(new MAIndicator(makeConfig("MA5"), 5));	// MA5
		ctx.registerIndicator(new EMAIndicator(makeConfig("EMA5"), 5));	// EMA5
	}
	
	private Configuration makeConfig(String name) {
		ContractField c = ctx.getContract(params.indicatorSymbol);
		return Configuration.builder().contract(c).indicatorName(name).numOfUnits(ctx.numOfMinPerMergedBar()).build();
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
