package tech.xuanwu.northstar.strategy.common.model.state;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;

/**
 * 管理状态转移
 * @author KevinHuangwl
 *
 */
@Slf4j
@Data
@NoArgsConstructor
public class ModuleStateMachine {

	private ModuleState curState;
	
	private ModuleState originState;
	
	public ModuleStateMachine(ModuleState state) {
		this.originState = state;
		this.curState = state;
	}
	
	public ModuleState transformForm(ModuleEventType eventType) {
		switch(eventType) {
		case OPENING_SIGNAL_CREATED:
			if(curState != ModuleState.EMPTY) {
				throw new IllegalStateException("当前状态异常：" + curState);
			}
			originState = curState;
			setState(ModuleState.PLACING_ORDER);
			break;
		case CLOSING_SIGNAL_CREATED:
			if(!curState.isHolding()) {
				throw new IllegalStateException("当前状态异常：" + curState);
			}
			originState = curState;
			setState(ModuleState.PLACING_ORDER);
			break;
		case SIGNAL_RETAINED:
			if(curState != ModuleState.PLACING_ORDER) {
				throw new IllegalStateException("当前状态异常：" + curState);
			}
			setState(ModuleState.EMPTY);
			break;
		case ORDER_SUBMITTED:
			if(curState != ModuleState.PLACING_ORDER) {
				throw new IllegalStateException("当前状态异常：" + curState);
			}
			setState(ModuleState.PENDING_ORDER);
			break;
		case BUY_TRADED:
			if(!curState.isOrdering()) {
				throw new IllegalStateException("当前状态异常：" + curState);
			}
			setState(originState == ModuleState.EMPTY ? ModuleState.HOLDING_LONG : ModuleState.EMPTY);
			break;
		case SELL_TRADED:
			if(!curState.isOrdering()) {
				throw new IllegalStateException("当前状态异常：" + curState);
			}
			setState(originState == ModuleState.EMPTY ? ModuleState.HOLDING_SHORT : ModuleState.EMPTY);
			break;
		case RETRY_RISK_ALERTED:
			if(curState != ModuleState.PENDING_ORDER) {
				throw new IllegalStateException("当前状态异常：" + curState);
			}
			setState(ModuleState.RETRIEVING_FOR_RETRY);
			break;
		case REJECT_RISK_ALERTED:
			if(curState != ModuleState.PENDING_ORDER) {
				throw new IllegalStateException("当前状态异常：" + curState);
			}
			setState(ModuleState.RETRIEVING_FOR_CANCAL);
			break;
		case ORDER_CANCELLED:
			if(!curState.isOrdering()) {
				throw new IllegalStateException("当前状态异常：" + curState);
			}
			setState(curState == ModuleState.RETRIEVING_FOR_RETRY ? ModuleState.PLACING_ORDER : originState);
			break;
		case STOP_LOSS:
			setState(ModuleState.EMPTY);
			break;
		default:
			throw new IllegalStateException("未有" + eventType + "事件处理逻辑");
		}
		return curState;
	}
	
	private void setState(ModuleState newState) {
		log.info("状态机切换：[{}] => [{}]", curState, newState);
		curState = newState;
	}

	public ModuleState getState() {
		return curState;
	}
}
