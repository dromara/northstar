package tech.xuanwu.northstar.domain.strategy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tech.xuanwu.northstar.strategy.api.EventDrivenComponent;
import tech.xuanwu.northstar.strategy.api.RiskControlRule;
import tech.xuanwu.northstar.strategy.api.TickDataAware;
import tech.xuanwu.northstar.strategy.api.constant.RiskAuditResult;
import tech.xuanwu.northstar.strategy.api.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.api.event.ModuleEventBus;
import tech.xuanwu.northstar.strategy.api.event.ModuleEventType;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

public class RiskControlPolicy implements TickDataAware, EventDrivenComponent{
	
	private List<RiskControlRule> rules;
	
	private TickField lastTick;
	
	private ModuleEventBus meb;
	
	public RiskControlPolicy(List<RiskControlRule> rules) {
		this.rules = rules;
	}

	@Override
	public void onEvent(ModuleEvent<?> moduleEvent) {
		if(moduleEvent.getEventType() == ModuleEventType.ORDER_REQ_CREATED) {
			SubmitOrderReqField orderReq = (SubmitOrderReqField) moduleEvent.getData();
			if(orderReq.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
				if(lastTick == null) {
					meb.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_RETAINED, orderReq));
				}
				Set<RiskAuditResult> results = new HashSet<>();
				for(RiskControlRule rule : rules) {
					results.add(rule.checkRisk(orderReq, lastTick));
				}
				if(results.contains(RiskAuditResult.REJECTED) || results.contains(RiskAuditResult.RETRY)) {					
					meb.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_RETAINED, orderReq));
				} else if(results.contains(RiskAuditResult.ACCEPTED)) {
					meb.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_ACCEPTED, orderReq));
				}
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
