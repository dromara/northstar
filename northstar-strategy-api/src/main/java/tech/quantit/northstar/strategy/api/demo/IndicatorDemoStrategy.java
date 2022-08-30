package tech.quantit.northstar.strategy.api.demo;

import static tech.quantit.northstar.strategy.api.indicator.function.AverageFunctions.*;
import static tech.quantit.northstar.strategy.api.indicator.function.StatsFunctions.*;

import com.google.common.util.concurrent.AtomicDouble;

import tech.quantit.northstar.common.model.DynamicParams;
import tech.quantit.northstar.common.model.Setting;
import tech.quantit.northstar.strategy.api.AbstractStrategy;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.indicator.complex.ATR;
import tech.quantit.northstar.strategy.api.indicator.complex.BOLL;
import tech.quantit.northstar.strategy.api.indicator.complex.KDJ;
import tech.quantit.northstar.strategy.api.indicator.complex.LWR;
import tech.quantit.northstar.strategy.api.indicator.complex.RSI;
import tech.quantit.northstar.strategy.api.indicator.complex.WAVE;
import tech.quantit.northstar.strategy.api.indicator.function.ComputeFunctions;
import xyz.redtorch.pb.CoreField.BarField;

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
	public void onBar(BarField bar, boolean isModuleEnabled) {
		super.onBar(bar, isModuleEnabled);
	
		// 当夜盘时值为0，日盘时值为1
		valueHolder.set(bar.getActionDay().equals(bar.getTradingDay()) ? 1 : 0);
	}

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
		
		LWR lwr = LWR.of(9, 3, 3);
		ctx.newIndicator("LWR1", params.indicatorSymbol, lwr.fast());
		ctx.newIndicator("LWR2", params.indicatorSymbol, lwr.slow());
		
		WAVE waveShape = WAVE.of(20, 3);
		ctx.newIndicator("WAVE", params.indicatorSymbol, waveShape.wave());
		
		ctx.newIndicator("RSI1", params.indicatorSymbol, RSI.line(7));
		ctx.newIndicator("RSI2", params.indicatorSymbol, RSI.line(14));
		
		ctx.newIndicator("ATR1", params.indicatorSymbol, ATR.ofBar(20));
		ctx.newIndicator("ATR2", params.indicatorSymbol, ATR.ofDay(3));
		
		ctx.newIndicator("SMA", params.indicatorSymbol, SMA(20, 2));
		ctx.newIndicator("SETTLE", params.indicatorSymbol, SETTLE());
		ctx.newIndicator("WMA", params.indicatorSymbol, WMA(72));	// 加权均价
		ctx.newIndicator("HHV", params.indicatorSymbol, HHV(72));	// 最高价
		ctx.newIndicator("LLV", params.indicatorSymbol, LLV(72)); 	// 最低价

		// 复合指标
		ctx.newIndicator("WMA_HHV", params.indicatorSymbol, WMA(72).andThen(HHV(72))); 	// 加权均价的最高价
		ctx.newIndicator("WMA_LLV", params.indicatorSymbol, WMA(72).andThen(LLV(72))); 	// 加权均价的最高价
		
		ctx.newIndicator("VAL", params.indicatorSymbol, ComputeFunctions.display(valueHolder));
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
