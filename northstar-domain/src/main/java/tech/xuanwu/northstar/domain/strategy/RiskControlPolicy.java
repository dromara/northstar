package tech.xuanwu.northstar.domain.strategy;

import java.util.List;

import tech.xuanwu.northstar.strategy.api.EventDrivenComponent;
import tech.xuanwu.northstar.strategy.api.ModuleStatus;
import tech.xuanwu.northstar.strategy.api.RiskControlRule;
import tech.xuanwu.northstar.strategy.api.TickDataAware;
import tech.xuanwu.northstar.strategy.api.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.api.event.ModuleEventBus;
import tech.xuanwu.northstar.strategy.api.event.ModuleEventType;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

public class RiskControlPolicy implements TickDataAware, EventDrivenComponent{
	
	private List<RiskControlRule> rules;
	
	private TickField lastTick;
	
	private ModuleStatus moduleStatus;
	
	private ModuleEventBus meb;
	
	public RiskControlPolicy(ModuleStatus moduleStatus, List<RiskControlRule> rules) {
		this.rules = rules;
		this.moduleStatus = moduleStatus;
	}

	@Override
	public void onEvent(ModuleEvent<?> moduleEvent) {
		if(moduleEvent.getEventType() == ModuleEventType.ORDER_REQ_CREATED) {
			SubmitOrderReqField orderReq = (SubmitOrderReqField) moduleEvent.getData();
			if(orderReq.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
				if(lastTick == null) {
					meb.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_RETAINED, orderReq));
				}
				rules.stream().forEach(rule -> rule.checkRisk(orderReq, lastTick, moduleStatus));
			}
		}
	}

	@Override
	public void onTick(TickField tick) {
		lastTick = tick;
	}

	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		meb = moduleEventBus;
	}
	
}
