package org.dromara.northstar.strategy.example;

import static org.dromara.northstar.indicator.function.AverageFunctions.MA;

import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.IIndicator;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.Configuration;
import org.dromara.northstar.strategy.model.TradeIntent;

import xyz.redtorch.pb.CoreField.BarField;

/**
 * 本示例用于展示一个多周期指标的策略
 * 本示例演示一分钟与十五分钟双周期单合约的实现方案
 * 对一分钟周期与十五分钟周期都计算均线信号，只有同时出现多头信号才做多，反之亦然。为简化参数，两个周期使用相同的均线参数
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@StrategicComponent(MultiPeriodSampleStrategy.NAME)
public class MultiPeriodSampleStrategy extends AbstractStrategy	// 为了简化代码，引入一个通用的基础抽象类
	implements TradeStrategy{

	protected static final String NAME = "示例-多周期策略";	// 之所以要这样定义一个常量，是为了方便日志输出时可以带上策略
	
	private InitParams params;	// 策略的参数配置信息
	
	private IIndicator fastLine1;	// 主周期快线
	private IIndicator slowLine1;	// 主周期慢线
	private IIndicator fastLine2;	// 参考周期快线
	private IIndicator slowLine2;	// 参考周期慢线 
	
	@Override
	public void onMergedBar(BarField bar) {
		log.debug("{} K线数据： 开 [{}], 高 [{}], 低 [{}], 收 [{}]", 
				bar.getUnifiedSymbol(), bar.getOpenPrice(), bar.getHighPrice(), bar.getLowPrice(), bar.getClosePrice());
		// 确保指标已经准备好再开始交易
		boolean allLineReady = fastLine1.isReady() && slowLine1.isReady() && fastLine2.isReady() && slowLine2.isReady();
		if(!allLineReady) {
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
							.priceType(PriceType.ANY_PRICE)
							.volume(1)
							.timeout(3000)
							.build());
				}
				if(shouldSell()) {
					ctx.submitOrderReq(TradeIntent.builder()
							.contract(ctx.getContract(bar.getUnifiedSymbol()))
							.operation(SignalOperation.SELL_OPEN)
							.priceType(PriceType.ANY_PRICE)
							.volume(1)
							.timeout(3000)
							.build());
				}
					
			}
			case HOLDING_LONG -> {
				if(fastLine1.value(0) < slowLine1.value(0)) {
					ctx.submitOrderReq(TradeIntent.builder()
							.contract(ctx.getContract(bar.getUnifiedSymbol()))
							.operation(SignalOperation.SELL_CLOSE)
							.priceType(PriceType.ANY_PRICE)
							.volume(1)
							.timeout(3000)
							.build());
				}
			}
			case HOLDING_SHORT -> {
				if(fastLine1.value(0) > slowLine1.value(0)) {
					ctx.submitOrderReq(TradeIntent.builder()
							.contract(ctx.getContract(bar.getUnifiedSymbol()))
							.operation(SignalOperation.BUY_CLOSE)
							.priceType(PriceType.ANY_PRICE)
							.volume(1)
							.timeout(3000)
							.build());
				}
			}
			default -> { /* 其他情况不处理 */}
		}
	}

	private boolean shouldBuy() {
		return fastLine1.value(0) > slowLine1.value(0) && this.fastLine2.value(0) > this.slowLine2.value(0);
	}
	
	private boolean shouldSell() {
		return fastLine1.value(0) < slowLine1.value(0) && this.fastLine2.value(0) < this.slowLine2.value(0);
	}
	
	@Override
	protected void initIndicators() {
		// 主周期线
		this.fastLine1 = ctx.newIndicator(Configuration.builder()
				.indicatorName("快线")
				.bindedContract(ctx.getContract(params.indicatorSymbol))
				.build(), MA(params.fast));
		this.slowLine1 = ctx.newIndicator(Configuration.builder()
				.indicatorName("慢线")
				.bindedContract(ctx.getContract(params.indicatorSymbol))
				.build(), MA(params.slow));
		
		// 参考周期线
		this.fastLine2 = ctx.newIndicator(Configuration.builder()
				.indicatorName("快线")
				.numOfUnits(params.refPeriod)
				.bindedContract(ctx.getContract(params.indicatorSymbol))
				.build(), MA(params.fast));
		this.slowLine2 = ctx.newIndicator(Configuration.builder()
				.indicatorName("慢线")
				.numOfUnits(params.refPeriod)
				.plotPerBar(true)
				.bindedContract(ctx.getContract(params.indicatorSymbol))
				.build(), MA(params.slow));
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
		
		@Setting(label="快线周期", type = FieldType.NUMBER, order=1)		
		private int fast;						
		
		@Setting(label="慢线周期", type = FieldType.NUMBER, order=2)		
		private int slow;
		
		@Setting(label="参考周期", type = FieldType.NUMBER, order=10)
		private int refPeriod;
	}

}
