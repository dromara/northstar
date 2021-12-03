package tech.xuanwu.northstar.domain.strategy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.api.EventDrivenComponent;
import tech.xuanwu.northstar.strategy.api.RiskControlRule;
import tech.xuanwu.northstar.strategy.api.StateChangeListener;
import tech.xuanwu.northstar.strategy.api.TickDataAware;
import tech.xuanwu.northstar.strategy.api.constant.ModuleState;
import tech.xuanwu.northstar.strategy.api.constant.RiskAuditResult;
import tech.xuanwu.northstar.strategy.api.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.api.event.ModuleEventBus;
import tech.xuanwu.northstar.strategy.api.event.ModuleEventType;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public class RiskControlPolicy implements TickDataAware, EventDrivenComponent, StateChangeListener{
	
	private List<RiskControlRule> rules;
	
	private TickField lastTick;
	
	private ModuleEventBus meb;
	
	private ModuleState curState;
	
	private SubmitOrderReqField currentOrderReq;
	
	private String moduleName;
	
	public RiskControlPolicy(String moduleName, List<RiskControlRule> rules) {
		this.rules = rules;
		this.moduleName = moduleName;
	}

	@Override
	public void onEvent(ModuleEvent<?> moduleEvent) {
		if(moduleEvent.getEventType() == ModuleEventType.ORDER_REQ_CREATED) {
			currentOrderReq = (SubmitOrderReqField) moduleEvent.getData();
			if(currentOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
				if(lastTick == null) {
					meb.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_RETAINED, currentOrderReq));
					log.warn("[{}] 当前行情为空，无法计算风控", moduleName);
					return;
				}
				Set<RiskAuditResult> results = new HashSet<>();
				for(RiskControlRule rule : rules) {
					results.add(rule.checkRisk(currentOrderReq, lastTick));
				}
				if(results.contains(RiskAuditResult.REJECTED) || results.contains(RiskAuditResult.RETRY)) {					
					meb.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_RETAINED, currentOrderReq));
					log.warn("[{}] 风控限制，无法继续下单", moduleName);
				} else if(results.contains(RiskAuditResult.ACCEPTED)) {
					meb.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_ACCEPTED, currentOrderReq));
				}
			}
		}
	}

	@Override
	public void onTick(TickField tick) {
		lastTick = tick;
		if(curState == ModuleState.PENDING_ORDER) {
			Set<RiskAuditResult> results = new HashSet<>();
			for(RiskControlRule rule : rules) {
				results.add(rule.checkRisk(currentOrderReq, lastTick));
			}
			if(results.contains(RiskAuditResult.REJECTED)) {					
				meb.post(new ModuleEvent<>(ModuleEventType.REJECT_RISK_ALERTED, currentOrderReq));
				log.warn("[{}] 风控限制，需要撤单取消交易", moduleName);
			} else if(results.contains(RiskAuditResult.RETRY)) {
				meb.post(new ModuleEvent<>(ModuleEventType.RETRY_RISK_ALERTED, currentOrderReq));
				log.warn("[{}] 风控限制，需要撤单重试交易", moduleName);
			}
		}
	}

	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		meb = moduleEventBus;
	}

	@Override
	public void onChange(ModuleState state) {
		curState = state;
		if(curState.isEmpty() || curState.isHolding()) {
			currentOrderReq = null;
		}
	}
	
}
