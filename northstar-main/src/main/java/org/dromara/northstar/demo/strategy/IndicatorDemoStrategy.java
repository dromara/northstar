package org.dromara.northstar.demo.strategy;

import static org.dromara.northstar.indicator.function.AverageFunctions.SETTLE;
import static org.dromara.northstar.indicator.function.AverageFunctions.SMA;
import static org.dromara.northstar.indicator.function.AverageFunctions.WMA;
import static org.dromara.northstar.indicator.function.StatsFunctions.HHV;
import static org.dromara.northstar.indicator.function.StatsFunctions.LLV;

import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.indicator.complex.ATR;
import org.dromara.northstar.indicator.complex.BOLL;
import org.dromara.northstar.indicator.complex.KDJ;
import org.dromara.northstar.indicator.complex.LWR;
import org.dromara.northstar.indicator.complex.MACD;
import org.dromara.northstar.indicator.complex.PBX;
import org.dromara.northstar.indicator.complex.RSI;
import org.dromara.northstar.indicator.complex.WAVE;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.model.Indicator.Configuration;

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
		// BOLL指标
		BOLL boll = BOLL.of(20, 2);
		ContractField c = ctx.getContract(params.indicatorSymbol);
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("BOLL_UPPER").bindedContract(c).build(), boll.upper());
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("BOLL_LOWER").bindedContract(c).build(), boll.lower());
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("BOLL_MID").bindedContract(c).build(), boll.mid());
		
		KDJ kdj = KDJ.of(9, 3, 3);
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("K").bindedContract(c).build(), kdj.k());
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("D").bindedContract(c).build(), kdj.d());
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("J").bindedContract(c).build(), kdj.j());
		
		LWR lwr = LWR.of(9, 3, 3);
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("LWR1").bindedContract(c).build(), lwr.fast());
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("LWR2").bindedContract(c).build(), lwr.slow());
		
		MACD macd = MACD.of(12, 26, 9);
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("MACD_DIF").bindedContract(c).build(), macd.diff());
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("MACD_DEA").bindedContract(c).build(), macd.dea());
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("MACD").bindedContract(c).build(), macd.post());
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("WAVE1").bindedContract(c).build(), WAVE.wr(20, 3));
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("WAVE2").bindedContract(c).build(), WAVE.macd(10, 20, 3));
		
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("RSI1").bindedContract(c).build(), RSI.line(7));
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("RSI2").bindedContract(c).build(), RSI.line(14));
		
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("ATR").bindedContract(c).build(), ATR.of(20));
		
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("SMA").bindedContract(c).build(), SMA(20, 2));
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("SETTLE").bindedContract(c).build(), SETTLE());
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("WMA").bindedContract(c).build(), WMA(72));
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("HHV").bindedContract(c).build(), HHV(72));
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("LLV").bindedContract(c).build(), LLV(72));
		
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("PB1").bindedContract(c).build(), PBX.line(4));
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("PB2").bindedContract(c).build(), PBX.line(6));
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("PB3").bindedContract(c).build(), PBX.line(9));
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("PB4").bindedContract(c).build(), PBX.line(13));
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("PB5").bindedContract(c).build(), PBX.line(18));
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("PB6").bindedContract(c).build(), PBX.line(24));
		
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("PBW1").bindedContract(c).build(), PBX.wline(4));
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("PBW2").bindedContract(c).build(), PBX.wline(6));
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("PBW3").bindedContract(c).build(), PBX.wline(9));
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("PBW4").bindedContract(c).build(), PBX.wline(13));
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("PBW5").bindedContract(c).build(), PBX.wline(18));
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("PBW6").bindedContract(c).build(), PBX.wline(24));

		// 复合指标
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("WMA_HHV").bindedContract(c).build(), WMA(72).andThen(HHV(72)));
		ctx.newIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("WMA_LLV").bindedContract(c).build(), WMA(72).andThen(LLV(72)));
		
		ctx.viewValueAsIndicator(Configuration.builder().numOfUnits(ctx.numOfMinPerModuleBar()).indicatorName("VAL").bindedContract(c).build(), valueHolder);
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
