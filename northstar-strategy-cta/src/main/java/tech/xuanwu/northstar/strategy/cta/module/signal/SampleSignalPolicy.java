package tech.xuanwu.northstar.strategy.cta.module.signal;

import java.util.Optional;

import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.model.data.BarData;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreField.TickField;

@StrategicComponent("示例策略")
public class SampleSignalPolicy implements SignalPolicy{
	
	/**
	 * 绑定合约
	 */
	private String bindedUnifiedSymbol;

	
	@Override
	public Optional<Signal> updateTick(TickField tick, BarData barData) {
		
		return null;
	}

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		bindedUnifiedSymbol = initParams.unifiedSymbol;
	}
	
	public class InitParams extends DynamicParams{
		
		@Label(value="绑定合约", order=1)
		private String unifiedSymbol;
		
	}
}
