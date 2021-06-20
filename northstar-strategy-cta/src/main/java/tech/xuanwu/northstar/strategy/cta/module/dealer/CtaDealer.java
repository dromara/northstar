package tech.xuanwu.northstar.strategy.cta.module.dealer;

import java.util.Optional;
import java.util.Set;

import com.alibaba.fastjson.JSON;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventBus;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;
import tech.xuanwu.northstar.strategy.common.model.CtaSignal;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
@StrategicComponent("CTA交易策略")
public class CtaDealer implements Dealer {
	
	private String bindedUnifiedSymbol;
	
	private ModuleEventBus moduleEventBus;
	
	private CtaSignal currentSignal;
	
	private SubmitOrderReqField currentOrderReq;
	
	@Override
	public Set<String> bindedUnifiedSymbols() {
		return Set.of(bindedUnifiedSymbol);
	}

	@Override
	public void onEvent(ModuleEvent event) {
		if(event.getEventType() == ModuleEventType.SIGNAL_CREATED) {
			currentSignal = (CtaSignal) event.getData();
			log.info("收到来自信号策略-[{}] 的信号：{}", currentSignal.getSignalClass().getName(), JSON.toJSONString(currentSignal));
			
		} else if(event.getEventType() == ModuleEventType.ORDER_RETRY) {
			currentOrderReq = (SubmitOrderReqField) event.getData();
			log.info("模组追单：{}", currentOrderReq);
			
		}
	}

	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		this.moduleEventBus = moduleEventBus;
	}

	@Override
	public Optional<SubmitOrderReqField> onTick(TickField tick) {
		// TODO Auto-generated method stub
		return Optional.ofNullable(null);
	}
	
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

}
