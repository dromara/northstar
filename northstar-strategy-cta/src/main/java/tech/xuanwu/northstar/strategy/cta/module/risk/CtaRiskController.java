package tech.xuanwu.northstar.strategy.cta.module.risk;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.RiskController;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.constants.RiskAuditResult;
import tech.xuanwu.northstar.strategy.common.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventBus;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;
import tech.xuanwu.northstar.strategy.common.model.ModuleAgent;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public class CtaRiskController implements RiskController{
	
	private ModuleAgent agent;
	
	private ModuleEventBus moduleEventBus;
	
	private List<RiskControlRule> rules;
	
	private SubmitOrderReqField currentOrderReq;
	
	public CtaRiskController(List<RiskControlRule> rules) {
		this.rules = rules;
	}

	@Override
	public void onEvent(ModuleEvent event) {
		if(event.getEventType() == ModuleEventType.ORDER_REQ_CREATED) {
			currentOrderReq = (SubmitOrderReqField) event.getData();
		}
	}

	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		this.moduleEventBus = moduleEventBus;
	}

	@Override
	public void onTick(TickField tick) {
		if(agent.getModuleState() == ModuleState.PLACING_ORDER || agent.getModuleState() == ModuleState.TRACING_ORDER) {
			short result = RiskAuditResult.ACCEPTED;
			// 只有开仓请求才需要风控审核
			if(currentOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_Open) {				
				for(RiskControlRule rule : rules) {
					result |= rule.canDeal(tick, agent);
				}
			}
			if((result & RiskAuditResult.REJECTED) == 1) {
				rejectOrder();
			} else if((result & RiskAuditResult.RETRY) == 1) {
				retryOrder();
			} else if(result == RiskAuditResult.ACCEPTED) {
				approveOrder();
			} else {
				log.warn("模组-[{}]，风控状态码为{}", agent.getName(), result);
				for(RiskControlRule rule : rules) {
					String name = rule.getClass().getAnnotation(StrategicComponent.class).value();
					log.warn("风控规则-[{}]，状态码为{}", name, rule.canDeal(tick, agent));
				}
			}
		}
	}

	@Override
	public void approveOrder() {
		moduleEventBus.post(ModuleEvent.builder()
				.eventType(ModuleEventType.ORDER_REQ_ACCEPTED)
				.data(currentOrderReq)
				.build());
	}

	@Override
	public void rejectOrder() {
		log.warn("模组-[{}]，下单请求被风控拒绝：{}", agent.getName(), currentOrderReq);
	}

	@Override
	public void retryOrder() {
		moduleEventBus.post(ModuleEvent.builder()
				.eventType(ModuleEventType.ORDER_RETRY)
				.data(currentOrderReq)
				.build());
	}

	@Override
	public void setModuleAgent(ModuleAgent agent) {
		this.agent = agent;
	}

}
