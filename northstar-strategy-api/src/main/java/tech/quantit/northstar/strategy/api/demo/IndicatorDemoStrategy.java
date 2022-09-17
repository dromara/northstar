package tech.quantit.northstar.strategy.api.demo;

import static tech.quantit.northstar.strategy.api.indicator.function.AverageFunctions.*;
import static tech.quantit.northstar.strategy.api.indicator.function.StatsFunctions.*;

import com.google.common.util.concurrent.AtomicDouble;

import tech.quantit.northstar.common.model.DynamicParams;
import tech.quantit.northstar.common.model.Setting;
import tech.quantit.northstar.strategy.api.AbstractStrategy;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.indicator.Indicator.Configuration;
import tech.quantit.northstar.strategy.api.indicator.complex.ATR;
import tech.quantit.northstar.strategy.api.indicator.complex.BOLL;
import tech.quantit.northstar.strategy.api.indicator.complex.KDJ;
import tech.quantit.northstar.strategy.api.indicator.complex.LWR;
import tech.quantit.northstar.strategy.api.indicator.complex.RSI;
import tech.quantit.northstar.strategy.api.indicator.complex.WAVE;
import tech.quantit.northstar.strategy.api.indicator.function.ComputeFunctions;
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
		ContractField c = ctx.getContract(params.indicatorSymbol);
		ctx.newIndicator(Configuration.builder().indicatorName("BOLL_UPPER").bindedContract(c).build(), boll.upper());
		ctx.newIndicator(Configuration.builder().indicatorName("BOLL_LOWER").bindedContract(c).build(), boll.lower());
		ctx.newIndicator(Configuration.builder().indicatorName("BOLL_MID").bindedContract(c).build(), boll.mid());
		
		KDJ kdj = KDJ.of(9, 3, 3);
		ctx.newIndicator(Configuration.builder().indicatorName("K").bindedContract(c).build(), kdj.k());
		ctx.newIndicator(Configuration.builder().indicatorName("D").bindedContract(c).build(), kdj.d());
		ctx.newIndicator(Configuration.builder().indicatorName("J").bindedContract(c).build(), kdj.j());
		
		LWR lwr = LWR.of(9, 3, 3);
		ctx.newIndicator(Configuration.builder().indicatorName("LWR1").bindedContract(c).build(), lwr.fast());
		ctx.newIndicator(Configuration.builder().indicatorName("LWR2").bindedContract(c).build(), lwr.slow());
		
		WAVE waveShape = WAVE.of(20, 3);
		ctx.newIndicator(Configuration.builder().indicatorName("WAVE").bindedContract(c).build(), waveShape.wave());
		
		ctx.newIndicator(Configuration.builder().indicatorName("RSI1").bindedContract(c).build(), RSI.line(7));
		ctx.newIndicator(Configuration.builder().indicatorName("RSI2").bindedContract(c).build(), RSI.line(14));
		
		ctx.newIndicator(Configuration.builder().indicatorName("ATR1").bindedContract(c).build(), ATR.ofBar(20));
		ctx.newIndicator(Configuration.builder().indicatorName("ATR2").bindedContract(c).build(), ATR.ofDay(3));
		
		ctx.newIndicator(Configuration.builder().indicatorName("SMA").bindedContract(c).build(), SMA(20, 2));
		ctx.newIndicator(Configuration.builder().indicatorName("SETTLE").bindedContract(c).build(), SETTLE());
		ctx.newIndicator(Configuration.builder().indicatorName("WMA").bindedContract(c).build(), WMA(72));
		ctx.newIndicator(Configuration.builder().indicatorName("HHV").bindedContract(c).build(), HHV(72));
		ctx.newIndicator(Configuration.builder().indicatorName("LLV").bindedContract(c).build(), LLV(72));

		// 复合指标
		ctx.newIndicator(Configuration.builder().indicatorName("WMA_HHV").bindedContract(c).build(), WMA(72).andThen(HHV(72)));
		ctx.newIndicator(Configuration.builder().indicatorName("WMA_LLV").bindedContract(c).build(), WMA(72).andThen(LLV(72)));
		
		ctx.newIndicator(Configuration.builder().indicatorName("VAL").bindedContract(c).build(), ComputeFunctions.display(valueHolder));
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
