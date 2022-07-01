package tech.quantit.northstar.external.indicator;

import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.model.DynamicParams;
import tech.quantit.northstar.common.model.Setting;
import tech.quantit.northstar.strategy.api.AbstractStrategy;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import tech.quantit.northstar.strategy.api.indicator.Indicator;
import tech.quantit.northstar.strategy.api.indicator.function.AverageFunctions;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 本示例用于展示外置策略
 * 采用的是简单的均线策略：快线在慢线之上做多，快线在慢线之下做空
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@StrategicComponent(ExternalSampleStrategy.NAME)
public class ExternalSampleStrategy extends AbstractStrategy	// 为了简化代码，引入一个通用的基础抽象类
	implements TradeStrategy{

	protected static final String NAME = "示例外置策略";
	
	private InitParams params;	// 策略的参数配置信息
	
	private Indicator fastLine;
	
	private Indicator slowLine;
	
	private Indicator settleLine;
	
	@Override
	protected void onBar(BarField bar) {
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
					ctx.submitOrderReq(ctx.getContract(bar.getUnifiedSymbol()), SignalOperation.BUY_OPEN, PriceType.ANY_PRICE, 1, 0);
					log.info("[{} {}] {}", ctx.getModuleName(), NAME, SignalOperation.BUY_OPEN.text());
				}
				if(shouldSell()) {
					ctx.submitOrderReq(ctx.getContract(bar.getUnifiedSymbol()), SignalOperation.SELL_OPEN, PriceType.ANY_PRICE, 1, 0);
					log.info("[{} {}] {}", ctx.getModuleName(), NAME, SignalOperation.BUY_OPEN.text());
				}
					
			}
			case HOLDING_LONG -> {
				if(fastLine.value(0) < slowLine.value(0)) {
					ctx.submitOrderReq(ctx.getContract(bar.getUnifiedSymbol()), SignalOperation.SELL_CLOSE, PriceType.ANY_PRICE, 1, 0);
					log.info("[{} {}] 平多", ctx.getModuleName(), NAME);
				}
			}
			case HOLDING_SHORT -> {
				if(fastLine.value(0) > slowLine.value(0)) {
					ctx.submitOrderReq(ctx.getContract(bar.getUnifiedSymbol()), SignalOperation.BUY_CLOSE, PriceType.ANY_PRICE, 1, 0);
					log.info("[{} {}] 平空", ctx.getModuleName(), NAME);
				}
			}
			default -> { /* 其他情况不处理 */}
		}
	}
	
	private boolean shouldBuy() {
		return fastLine.value(0) > slowLine.value(0) && fastLine.value(0) > settleLine.value(0);
	}
	
	private boolean shouldSell() {
		return fastLine.value(0) < slowLine.value(0) && fastLine.value(0) < settleLine.value(0);
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
		// 简单指标的创建 
		this.fastLine = ctx.newIndicator("快线", params.indicatorSymbol, AverageFunctions.MA(params.fast));
		this.slowLine = ctx.newIndicator("慢线", params.indicatorSymbol, AverageFunctions.MA(params.slow));
		// 成交量加权均价线
		this.settleLine = ctx.newIndicator("加权线", params.indicatorSymbol, AverageFunctions.SETTLE());
	}

	public static class InitParams extends DynamicParams {			
		
		@Setting(value="指标合约", order=0)
		private String indicatorSymbol;
		
		@Setting(value="快线周期", order=1)		
		private int fast;						
		
		@Setting(value="慢线周期", order=2)		
		private int slow;
		
	}
	
}
