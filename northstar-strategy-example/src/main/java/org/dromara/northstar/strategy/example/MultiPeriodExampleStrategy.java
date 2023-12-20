package org.dromara.northstar.strategy.example;

import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.utils.TradeHelper;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.trend.MAIndicator;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.slf4j.Logger;

/**
 * 本示例用于展示一个多周期指标的策略
 * 本示例演示一分钟与十五分钟双周期单合约的实现方案
 * 对一分钟周期与十五分钟周期都计算均线信号，只有同时出现多头信号才做多，反之亦然。为简化参数，两个周期使用相同的均线参数
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@StrategicComponent(MultiPeriodExampleStrategy.NAME)
public class MultiPeriodExampleStrategy extends AbstractStrategy	// 为了简化代码，引入一个通用的基础抽象类
	implements TradeStrategy{

	protected static final String NAME = "示例-多周期策略";	// 之所以要这样定义一个常量，是为了方便日志输出时可以带上策略
	
	private InitParams params;	// 策略的参数配置信息
	
	private Indicator fastLine1;	// 主周期快线
	private Indicator slowLine1;	// 主周期慢线
	private Indicator fastLine2;	// 参考周期快线
	private Indicator slowLine2;	// 参考周期慢线 
	private TradeHelper helper;
	private Logger logger;
	
	@Override
	public void onMergedBar(Bar bar) {
		logger.debug("{} K线数据： 开 [{}], 高 [{}], 低 [{}], 收 [{}]", 
				bar.contract().unifiedSymbol(), bar.openPrice(), bar.highPrice(), bar.lowPrice(), bar.closePrice());
		// 确保指标已经准备好再开始交易
		boolean allLineReady = fastLine1.isReady() && slowLine1.isReady() && fastLine2.isReady() && slowLine2.isReady();
		if(!allLineReady) {
			logger.debug("指标未准备就绪");
			return;
		}
		switch (ctx.getState()) {
			case EMPTY -> {
				// 快线在慢线之上开多，快线在慢线之下开空
				if(shouldBuy()) {					
					helper.doBuyOpen(1);
				}
				if(shouldSell()) {
					helper.doSellOpen(1);
				}
					
			}
			case HOLDING_LONG -> {
				if(fastLine1.value(0) < slowLine1.value(0)) {
					helper.doSellClose(1);
				}
			}
			case HOLDING_SHORT -> {
				if(fastLine1.value(0) > slowLine1.value(0)) {
					helper.doBuyClose(1);
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
		logger = ctx.getLogger(getClass());
		Contract c = ctx.getContract(params.indicatorSymbol);
		// 主周期线
		this.fastLine1 = new MAIndicator(Configuration.builder()
				.indicatorName("快线")
				.contract(c)
				.numOfUnits(ctx.numOfMinPerMergedBar())
				.build(), params.fast);
		this.slowLine1 = new MAIndicator(Configuration.builder()
				.indicatorName("慢线")
				.contract(c)
				.numOfUnits(ctx.numOfMinPerMergedBar())
				.build(), params.slow);
		
		// 参考周期线
		this.fastLine2 = new MAIndicator(Configuration.builder()
				.indicatorName("快线")
				.contract(c)
				.numOfUnits(params.refPeriod)
				.build(), params.fast);
		this.slowLine2 = new MAIndicator(Configuration.builder()
				.indicatorName("慢线")
				.contract(c)
				.numOfUnits(params.refPeriod)
				.build(), params.slow);
		
		ctx.registerIndicator(fastLine1);
		ctx.registerIndicator(fastLine2);
		ctx.registerIndicator(slowLine1);
		ctx.registerIndicator(slowLine2);
		
		helper = TradeHelper.builder().context(getContext()).tradeContract(c).build();
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
		private int fast = 5;						
		
		@Setting(label="慢线周期", type = FieldType.NUMBER, order=2)		
		private int slow = 10;
		
		@Setting(label="参考周期", type = FieldType.NUMBER, order=10)
		private int refPeriod = 15;
	}

}
