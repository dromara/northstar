#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.strategy;

import lombok.extern.slf4j.Slf4j;
import ${groupId}.northstar.common.constant.SignalOperation;
import ${groupId}.northstar.common.model.DynamicParams;
import ${groupId}.northstar.common.model.Setting;
import ${groupId}.northstar.strategy.api.AbstractStrategy;
import ${groupId}.northstar.strategy.api.TradeStrategy;
import ${groupId}.northstar.strategy.api.annotation.StrategicComponent;
import ${groupId}.northstar.strategy.api.constant.PriceType;
import ${groupId}.northstar.strategy.api.indicator.Indicator;
import ${groupId}.northstar.strategy.api.indicator.Indicator.ValueType;
import ${groupId}.northstar.strategy.api.indicator.MovingAverage;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 本示例源自于示例指标策略，用于展示自定义项目生成的情况下，如何使用自定义项目。
 * 采用的是简单的均线策略：快线在慢线之上做多，快线在慢线之下做空
 * 
 *  * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent(DemoExternalStrategy.NAME)
public class DemoExternalStrategy extends AbstractStrategy	// 为了简化代码，引入一个通用的基础抽象类
	implements TradeStrategy{

	protected static final String NAME = "示例外部策略";
	
	private Indicator fastLine;
	
	private Indicator slowLine;
	
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
				SignalOperation opr = fastLine.value(0) > slowLine.value(0) ? SignalOperation.BUY_OPEN : SignalOperation.SELL_OPEN;
				ctx.submitOrderReq(ctx.getContract(bar.getUnifiedSymbol()), opr, PriceType.ANY_PRICE, 1, 0);
				log.info("[{} {}] {}", ctx.getModuleName(), NAME, opr.text());
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

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams settings = (InitParams) params;
		// 指标的创建 
		this.fastLine = new MovingAverage(settings.indicatorSymbol, settings.fast, ValueType.CLOSE);
		this.slowLine = new MovingAverage(settings.indicatorSymbol, settings.slow, ValueType.CLOSE);
		// 指标创建后，必须加入指标集才能自动更新
		indicatorMap.put("快线", fastLine);
		indicatorMap.put("慢线", slowLine);
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