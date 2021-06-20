package tech.xuanwu.northstar.strategy.cta.module.dealer;

import java.util.Optional;
import java.util.Set;

import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventBus;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@StrategicComponent("CTA交易策略")
public class CtaDealer implements Dealer {
	
	private String bindedUnifiedSymbol;
	

	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.bindedUnifiedSymbol = initParams.bindedUnifiedSymbol;
	}
	
	public static class InitParams extends DynamicParams{

		@Label(value="绑定合约")
		private String bindedUnifiedSymbol;
	}

	@Override
	public Set<String> bindedUnifiedSymbols() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onEvent(ModuleEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSignal(Signal signal) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Optional<SubmitOrderReqField> onTick(TickField tick) {
		// TODO Auto-generated method stub
		return null;
	}

}
