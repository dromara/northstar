package tech.quantit.northstar.domain.strategy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.strategy.api.ContractBindedAware;
import tech.quantit.northstar.strategy.api.EventDrivenComponent;
import tech.quantit.northstar.strategy.api.RiskControlRule;
import tech.quantit.northstar.strategy.api.StateChangeListener;
import tech.quantit.northstar.strategy.api.Subscribable;
import tech.quantit.northstar.strategy.api.TickDataAware;
import tech.quantit.northstar.strategy.api.constant.ModuleState;
import tech.quantit.northstar.strategy.api.constant.RiskAuditResult;
import tech.quantit.northstar.strategy.api.event.ModuleEvent;
import tech.quantit.northstar.strategy.api.event.ModuleEventBus;
import tech.quantit.northstar.strategy.api.event.ModuleEventType;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public class RiskControlPolicy implements TickDataAware, EventDrivenComponent, StateChangeListener, ContractBindedAware{
	
	private List<RiskControlRule> rules;
	
	protected TickField lastTick;
	
	private ModuleEventBus meb;
	
	private ModuleState curState;
	
	protected SubmitOrderReqField currentOrderReq;
	
	private String moduleName;
	
	private ContractField bindedContract;
	
	private Set<RiskAuditResult> riskCheckResults = new HashSet<>();
	
	public RiskControlPolicy(String moduleName, List<RiskControlRule> rules) {
		this.rules = rules;
		this.moduleName = moduleName;
	}

	@Override
	public void onEvent(ModuleEvent<?> moduleEvent) {
		if(moduleEvent.getEventType() == ModuleEventType.ORDER_REQ_CREATED) {
			currentOrderReq = (SubmitOrderReqField) moduleEvent.getData();
			log.debug("[{}] 收到新建订单", moduleName);
			if(currentOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
				if(lastTick == null) {
					meb.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_RETAINED, currentOrderReq));
					log.warn("[{}] 当前行情为空，无法计算风控", moduleName);
					return;
				}
				for(RiskControlRule rule : rules) {
					riskCheckResults.add(rule.checkRisk(currentOrderReq, lastTick));
				}
				if(riskCheckResults.contains(RiskAuditResult.REJECTED) || riskCheckResults.contains(RiskAuditResult.RETRY)) {					
					meb.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_RETAINED, currentOrderReq));
					log.warn("[{}] 风控限制，无法继续下单", moduleName);
				} else if(riskCheckResults.contains(RiskAuditResult.ACCEPTED)) {
					meb.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_ACCEPTED, currentOrderReq));
					log.info("[{}] 订单过审 单号{} 合约{} 方向{} 手数{} 价格{} 止损{}", moduleName, currentOrderReq.getOriginOrderId(),
							currentOrderReq.getContract().getUnifiedSymbol(), currentOrderReq.getDirection(),
							currentOrderReq.getVolume(), currentOrderReq.getPrice(), currentOrderReq.getStopPrice());
				}
				riskCheckResults.clear();
			}
		}
	}

	@Override
	public void onTick(TickField tick) {
		if(!tick.getUnifiedSymbol().equals(bindedContractSymbol())) {
			return;
		}
		lastTick = tick;
		if(curState == ModuleState.PENDING_ORDER && currentOrderReq != null 
				&& StringUtils.equals(tick.getUnifiedSymbol(), currentOrderReq.getContract().getUnifiedSymbol())) {
			for(RiskControlRule rule : rules) {
				riskCheckResults.add(rule.checkRisk(currentOrderReq, lastTick));
			}
			if(riskCheckResults.contains(RiskAuditResult.REJECTED)) {					
				meb.post(new ModuleEvent<>(ModuleEventType.REJECT_RISK_ALERTED, currentOrderReq));
				log.warn("[{}] 风控限制，需要撤单取消交易", moduleName);
			} else if(riskCheckResults.contains(RiskAuditResult.RETRY)) {
				meb.post(new ModuleEvent<>(ModuleEventType.RETRY_RISK_ALERTED, currentOrderReq));
				log.warn("[{}] 风控限制，需要撤单重试交易", moduleName);
			}
			riskCheckResults.clear();
		}
	}

	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		meb = moduleEventBus;
		for(RiskControlRule rule : rules) {
			if(rule instanceof Subscribable sub) 				
				meb.register(sub);
			if(rule instanceof EventDrivenComponent edc) 
				edc.setEventBus(meb);
		}
	}

	@Override
	public void onChange(ModuleState state) {
		curState = state;
		if(curState.isEmpty() || curState.isHolding()) {			
			currentOrderReq = null;
		}
		
		for(RiskControlRule rule : rules) {
			if(rule instanceof StateChangeListener listener) 
				listener.onChange(state);
		}
	}

	@Override
	public String bindedContractSymbol() {
		return bindedContract.getUnifiedSymbol();
	}

	@Override
	public void setBindedContract(ContractField contract) {
		bindedContract = contract;
	}
	
}
