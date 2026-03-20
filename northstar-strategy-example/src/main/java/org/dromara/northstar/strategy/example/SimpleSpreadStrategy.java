package org.dromara.northstar.strategy.example;

import java.util.concurrent.ThreadLocalRandom;

import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.TradeHelper;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.slf4j.Logger;

import lombok.Setter;

@StrategicComponent(SimpleSpreadStrategy.NAME)
public class SimpleSpreadStrategy extends AbstractStrategy implements TradeStrategy{

	protected static final String NAME = "示例-简单价差策略";
	
	private InitParams params;	// 策略的参数配置信息
	
	private Logger logger;
	
	private TradeHelper hlp1;
	private TradeHelper hlp2;
	
	private long nextActionTime;
	
	private int holding;
	
	@Setter
	public static class InitParams extends DynamicParams {			
		
		@Setting(label="合约1", order = 10)
		private String unifiedSymbol1;
		
		@Setting(label="合约2", order = 20)
		private String unifiedSymbol2;
		
	}
	
	/***************** 以下如果看不懂，基本可以照搬 *************************/
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		this.params = (InitParams) params;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	protected void initIndicators() {
		Contract c1 = ctx.getContract(params.unifiedSymbol1);
		Contract c2 = ctx.getContract(params.unifiedSymbol2);
		logger = ctx.getLogger(getClass());
		hlp1 = new TradeHelper(ctx, c1);
		hlp2 = new TradeHelper(ctx, c2);
	}

	@Override
	public void onTick(Tick tick) {
		if(tick.contract().unifiedSymbol().equals(params.unifiedSymbol2)) {
			return;
		}
		long now = tick.actionTimestamp();
		// 启用后，等待10秒才开始交易
		if(nextActionTime == 0) {
			nextActionTime = now + 10000;
		}
		if(now > nextActionTime) {
			nextActionTime = now + 60000;
			logger.info("开始交易");
			if(holding == 0) {
				if(ThreadLocalRandom.current().nextBoolean()) {
					holding = 1;
					hlp1.doBuyOpen(1);
					hlp2.doSellOpen(1);
				} else {
					holding = -1;
					hlp1.doSellOpen(1);
					hlp2.doBuyOpen(1);
				}
			} else {
				if(holding > 0) {
					hlp1.doSellClose(1);
					hlp2.doBuyClose(1);
				} else {
					hlp1.doBuyClose(1);
					hlp2.doSellClose(1);
				}
				holding = 0;
			}
		}
	}
	
	@Override
	public ModuleType type() {
		return ModuleType.ARBITRAGE;	// 套利策略专有标识 
	}
}
