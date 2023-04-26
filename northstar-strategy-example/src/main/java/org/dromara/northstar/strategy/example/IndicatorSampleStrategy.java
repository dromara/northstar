package org.dromara.northstar.strategy.example;

import static org.dromara.northstar.indicator.function.AverageFunctions.*;
import static org.dromara.northstar.indicator.function.ComputeFunctions.*;

import org.dromara.northstar.indicator.complex.MACD;
import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.IIndicator;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PeriodUnit;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.Configuration;
import org.dromara.northstar.strategy.model.TradeIntent;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 本示例用于展示一个带指标的策略
 * 采用的是简单的均线策略：快线在慢线之上做多，快线在慢线之下做空
 *
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@StrategicComponent(IndicatorSampleStrategy.NAME)
public class IndicatorSampleStrategy extends AbstractStrategy	// 为了简化代码，引入一个通用的基础抽象类
		implements TradeStrategy{

	protected static final String NAME = "示例-指标策略";

	private InitParams params;	// 策略的参数配置信息

	private IIndicator fastLine;

	private IIndicator slowLine;

	private IIndicator macdDiff;

	private IIndicator macdDea;

	@Override
	public void onMergedBar(BarField bar) {
		log.debug("{} K线数据： 开 [{}], 高 [{}], 低 [{}], 收 [{}]",
				bar.getUnifiedSymbol(), bar.getOpenPrice(), bar.getHighPrice(), bar.getLowPrice(), bar.getClosePrice());
		// 确保指标已经准备好再开始交易
		if(!fastLine.isReady() || !slowLine.isReady()) {
			log.debug("指标未准备就绪");
			return;
		}
		switch (ctx.getState()) {
			case EMPTY -> {
				// 快线在慢线之上开多，快线在慢线之下开空
				if(shouldBuy()) {
					ctx.submitOrderReq(TradeIntent.builder()
							.contract(ctx.getContract(bar.getUnifiedSymbol()))
							.operation(SignalOperation.BUY_OPEN)
							.priceType(PriceType.OPP_PRICE)
							.volume(1)
							.timeout(5000)
							.build());
					log.info("多开");
				}
				if(shouldSell()) {
					ctx.submitOrderReq(TradeIntent.builder()
							.contract(ctx.getContract(bar.getUnifiedSymbol()))
							.operation(SignalOperation.SELL_OPEN)
							.priceType(PriceType.OPP_PRICE)
							.volume(1)
							.timeout(5000)
							.build());
					log.info("空开");
				}

			}
			case HOLDING_LONG -> {
				if(fastLine.value(0) < slowLine.value(0)) {
					ctx.submitOrderReq(TradeIntent.builder()
							.contract(ctx.getContract(bar.getUnifiedSymbol()))
							.operation(SignalOperation.SELL_CLOSE)
							.priceType(PriceType.OPP_PRICE)
							.volume(1)
							.timeout(5000)
							.build());
					log.info("平多");
				}
			}
			case HOLDING_SHORT -> {
				if(fastLine.value(0) > slowLine.value(0)) {
					ctx.submitOrderReq(TradeIntent.builder()
							.contract(ctx.getContract(bar.getUnifiedSymbol()))
							.operation(SignalOperation.BUY_CLOSE)
							.priceType(PriceType.OPP_PRICE)
							.volume(1)
							.timeout(5000)
							.build());
					log.info("平空");
				}
			}
			default -> { /* 其他情况不处理 */}
		}
	}

	@Override
	public void onTick(TickField tick) {
		log.info("时间：{} {} 价格：{} 指标值：{}", tick.getActionDay(), tick.getActionTime(), tick.getLastPrice(), fastLine.value(0));
	}

	private boolean shouldBuy() {
		return fastLine.value(0) > slowLine.value(0) && this.macdDiff.value(0) > this.macdDea.value(0);
	}

	private boolean shouldSell() {
		return fastLine.value(0) < slowLine.value(0) && this.macdDiff.value(0) < this.macdDea.value(0);
	}

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		this.params = (InitParams) params;
	}

	@Override
	protected void initIndicators() {
//		// 简单指标的创建
//		this.fastLine = ctx.newIndicator(Configuration.builder()
//				.indicatorName("快线")
//				.bindedContract(ctx.getContract(params.indicatorSymbol))
//				.numOfUnits(ctx.numOfMinPerMergedBar())
//				.period(PeriodUnit.MINUTE)
//				.build(), MA(params.fast));
//		this.slowLine = ctx.newIndicator(Configuration.builder()
//				.indicatorName("慢线")
//				.bindedContract(ctx.getContract(params.indicatorSymbol))
//				.numOfUnits(ctx.numOfMinPerMergedBar())
//				.period(PeriodUnit.MINUTE)
//				.build(), MA(params.slow));
//
//		// 复杂指标的创建；MACD的原始写法
//		this.macdDiff = ctx.newIndicator(Configuration.builder()
//				.indicatorName("MACD_DIF")
//				.bindedContract(ctx.getContract(params.indicatorSymbol))
//				.numOfUnits(ctx.numOfMinPerMergedBar())
//				.period(PeriodUnit.MINUTE)
//				.build(), minus(EMA(12), EMA(26)));
//		this.macdDea = ctx.newIndicator(Configuration.builder()
//				.indicatorName("MACD_DEA")
//				.bindedContract(ctx.getContract(params.indicatorSymbol))
//				.numOfUnits(ctx.numOfMinPerMergedBar())
//				.period(PeriodUnit.MINUTE)
//				.build(), minus(EMA(12), EMA(26)).andThen(EMA(9)));
//
//		
//		//######## 以下写法仅用于监控台演示，因此没有赋值给类属性，同时为了简化参数也直接写死 ########//
//		
//		// MACD的另一种写法，对MACD的计算函数做进一步封装
//		MACD macd = MACD.of(12, 26, 9);
//		ctx.newIndicator(Configuration.builder()
//				.indicatorName("MACD_DIF2")
//				.bindedContract(ctx.getContract(params.indicatorSymbol))
//				.numOfUnits(ctx.numOfMinPerMergedBar())
//				.period(PeriodUnit.MINUTE)
//				.build(), macd.diff());
//		ctx.newIndicator(Configuration.builder()
//				.indicatorName("MACD_DEA2")
//				.bindedContract(ctx.getContract(params.indicatorSymbol))
//				.numOfUnits(ctx.numOfMinPerMergedBar())
//				.period(PeriodUnit.MINUTE)
//				.build(), macd.dea());
//		ctx.newIndicator(Configuration.builder()
//				.indicatorName("MACD_POST")
//				.bindedContract(ctx.getContract(params.indicatorSymbol))
//				.numOfUnits(ctx.numOfMinPerMergedBar())
//				.period(PeriodUnit.MINUTE)
//				.build(), macd.post());
	}

	public static class InitParams extends DynamicParams {			
		
		@Setting(label="指标合约", order=0)
		private String indicatorSymbol;
		
		@Setting(label="快线周期", type = FieldType.NUMBER, order=1)		
		private int fast;						
		
		@Setting(label="慢线周期", type = FieldType.NUMBER, order=2)		
		private int slow;

	}

}
