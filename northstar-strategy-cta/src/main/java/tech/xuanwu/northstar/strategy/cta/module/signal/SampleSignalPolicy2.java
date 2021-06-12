package tech.xuanwu.northstar.strategy.cta.module.signal;

import java.util.List;
import java.util.Optional;

import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.model.data.BarData;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreField.TickField;

@StrategicComponent("示例策略2")
public class SampleSignalPolicy2 implements SignalPolicy{
	
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
		
	}
	
	public class InitParams extends DynamicParams{
		
		@Label(value="绑定合约", order=10)
		private String unifiedSymbol;
		
		@Label(value="短周期", order=20, unit="天")
		private int shortPeriod;
		
		@Label(value="长周期", order=30, unit="天")
		private int longPeriod;

	}

	@Override
	public List<String> bindedUnifiedSymbols() {
		// TODO Auto-generated method stub
		return null;
	}
}
