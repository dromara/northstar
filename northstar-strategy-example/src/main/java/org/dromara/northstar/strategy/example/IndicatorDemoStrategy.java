package org.dromara.northstar.strategy.example;

import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.indicator.Configuration;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.helper.HHVIndicator;
import org.dromara.northstar.indicator.helper.LLVIndicator;
import org.dromara.northstar.indicator.helper.SimpleValueIndicator;
import org.dromara.northstar.indicator.momentum.KDIndicator;
import org.dromara.northstar.indicator.momentum.RSIIndicator;
import org.dromara.northstar.indicator.trend.EMAIndicator;
import org.dromara.northstar.indicator.trend.MACDIndicator;
import org.dromara.northstar.indicator.trend.MAIndicator;
import org.dromara.northstar.indicator.trend.SMAIndicator;
import org.dromara.northstar.indicator.volatility.BOLLIndicator;
import org.dromara.northstar.indicator.volatility.TrueRangeIndicator;
import org.dromara.northstar.indicator.volume.IntraDaySettlePriceIndicator;
import org.dromara.northstar.indicator.volume.VWAPIndicator;
import org.dromara.northstar.indicator.wave.MABasedWaveIndicator;
import org.dromara.northstar.indicator.wave.MABasedWaveIndicator.EndpointType;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.ValueType;

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
		ContractField c = ctx.getContract(params.indicatorSymbol);
		//######## 以下写法仅用于监控台演示，因此没有赋值给类属性，同时为了简化参数也直接写死 ########//
		// 指标的难度级数：基础（单值指标）
		ctx.registerIndicator(new MAIndicator(makeConfig("MA5"), 5));	// MA5
		ctx.registerIndicator(new EMAIndicator(makeConfig("EMA5"), 5));	// EMA5
		ctx.registerIndicator(new SMAIndicator(makeConfig("SMA10"), 10, 2));
		// 指标的难度级数：基础（指标嵌套）
		ctx.registerIndicator(new HHVIndicator(makeConfig("HHV"), 
				new SimpleValueIndicator(Configuration.builder().contract(c).valueType(ValueType.HIGH).cacheLength(10).visible(false).build())));	// 10个周期内的最高价
		ctx.registerIndicator(new LLVIndicator(makeConfig("LLV"),
				new SimpleValueIndicator(Configuration.builder().contract(c).valueType(ValueType.LOW).cacheLength(10).visible(false).build())));	// 10个周期内的最低价
		
		// 指标的难度级数：进阶（伪多值指标，内部手动更新值）
		ctx.registerIndicator(new RSIIndicator(makeConfig("RSI1"), 7));
		ctx.registerIndicator(new RSIIndicator(makeConfig("RSI2"), 14));
		// 指标的难度级数：进阶（多值指标，内部嵌套）
		ctx.registerIndicator(new MACDIndicator(makeConfig("MACD"), 12, 26, 9));
		ctx.registerIndicator(new BOLLIndicator(makeConfig("BOLL"), 20, 2));	
		// 指标的难度级数：进阶（控制日内计算，多值混算）
		ctx.registerIndicator(new IntraDaySettlePriceIndicator(makeConfig("SP")));	// 日内均价
		// 指标的难度级数：进阶（多值混算）
		ctx.registerIndicator(new VWAPIndicator(makeConfig("VWAP"), 100));			// 成交加权均价
		Indicator tr = new TrueRangeIndicator(makeConfig("TR"));
		Indicator atr = new MAIndicator(makeConfig("ATR"), tr, 26);
		ctx.registerIndicator(tr);
		ctx.registerIndicator(atr);
		
		// 指标的难度级数：精通（内部类定义内部实现）
		ctx.registerIndicator(new KDIndicator(makeConfig("KD"), 9, 3, 3));
		// 指标的难度级数：精通（输出抑制，回溯连接，自定枚举）
		ctx.registerIndicator(new MABasedWaveIndicator(makeConfig("WAVE"), 2, EndpointType.HIGH_LOW));
		
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
