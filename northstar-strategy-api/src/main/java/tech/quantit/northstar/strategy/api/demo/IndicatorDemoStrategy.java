package tech.quantit.northstar.strategy.api.demo;

import static tech.quantit.northstar.strategy.api.indicator.function.AverageFunctions.*;
import static tech.quantit.northstar.strategy.api.indicator.function.StatsFunctions.*;

import tech.quantit.northstar.common.model.DynamicParams;
import tech.quantit.northstar.common.model.Setting;
import tech.quantit.northstar.strategy.api.AbstractStrategy;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.indicator.complex.BOLL;
import tech.quantit.northstar.strategy.api.indicator.complex.KDJ;

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
	
	@Override
	protected void initIndicators() {
		//######## 以下写法仅用于监控台演示，因此没有赋值给类属性，同时为了简化参数也直接写死 ########//
		// BOLL指标
		BOLL boll = BOLL.of(20, 2);
		ctx.newIndicator("BOLL_UPPER",params.indicatorSymbol, boll.upper());
		ctx.newIndicator("BOLL_LOWER",params.indicatorSymbol, boll.lower());
		ctx.newIndicator("BOLL_MID",params.indicatorSymbol, boll.mid());
		
		KDJ kdj = KDJ.of(9, 3, 3);
		ctx.newIndicator("K", params.indicatorSymbol, kdj.k());
		ctx.newIndicator("D", params.indicatorSymbol, kdj.d());
		ctx.newIndicator("J", params.indicatorSymbol, kdj.j());
		
		ctx.newIndicator("SMA", params.indicatorSymbol, SMA(10, 2));
		ctx.newIndicator("WP", params.indicatorSymbol, SETTLE(72));	// 加权均价
		ctx.newIndicator("HHV", params.indicatorSymbol, HHV(72));	// 最高价
		ctx.newIndicator("LLV", params.indicatorSymbol, LLV(72)); 	// 最低价

		// 复合指标
		ctx.newIndicator("SETTLE_HHV", params.indicatorSymbol, SETTLE(72).andThen(HHV(72))); 	// 加权均价的最高价
		ctx.newIndicator("SETTLE_LLV", params.indicatorSymbol, SETTLE(72).andThen(LLV(72))); 	// 加权均价的最高价
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
