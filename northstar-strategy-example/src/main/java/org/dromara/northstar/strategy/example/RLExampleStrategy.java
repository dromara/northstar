package org.dromara.northstar.strategy.example;

import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;

/**
 * 本示例用于展示一个集成了强化学习的策略
 * 
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@StrategicComponent(RLExampleStrategy.NAME)
public class RLExampleStrategy extends AbstractStrategy	implements TradeStrategy{

	protected static final String NAME = "示例-强化学习策略";
	
	private InitParams params;	// 策略的参数配置信息

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
