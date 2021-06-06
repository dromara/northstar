package tech.xuanwu.northstar.strategy.cta.module.signal;

import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.model.BarData;
import tech.xuanwu.northstar.strategy.common.model.DynamicParams;
import tech.xuanwu.northstar.strategy.common.model.Signal;
import xyz.redtorch.pb.CoreField.TickField;

@StrategicComponent("示例策略2")
public class SampleSignalPolicy2 implements SignalPolicy{
	
	/**
	 * 绑定合约
	 */
	private String bindedUnifiedSymbol;

	@Override
	public Signal updateTick(TickField tick, BarData barData) {
		return null;
	}
	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}


	@Override
	public void initWithParams(DynamicParams params) {
		
	}
	
	public class InitParams extends DynamicParams{
		
		@Label(value="绑定合约", order=10)
		private String unifiedSymbol;
		
		@Label(value="短周期", order=20, unit="天")
		private int shortPeriod;
		
		@Label(value="长周期", order=30, unit="天")
		private int longPeriod;

	}
}
