package tech.xuanwu.northstar.strategy.common.model.state;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;

/**
 * 管理状态转移
 * @author KevinHuangwl
 *
 */
@Slf4j
public class ModuleStateMachine {

	private ModuleState curState;
	
	private ModuleState originState;
	
	public ModuleStateMachine(ModuleState state) {
		this.originState = state;
	}
	
	public void transformForm(ModuleEventType eventType) {
		switch(eventType) {
		case SIGNAL_CREATED:
			if(curState != ModuleState.EMPTY && curState != ModuleState.HOLDING) {
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
		case ORDER_TRADED:
			if(curState != ModuleState.PENDING_ORDER) {
				throw new IllegalStateException("当前状态异常：" + curState);
			}
			setState(originState == ModuleState.EMPTY ? ModuleState.HOLDING : ModuleState.EMPTY);
			break;
		case RISK_ALERTED:
			if(curState != ModuleState.PENDING_ORDER) {
				throw new IllegalStateException("当前状态异常：" + curState);
			}
			setState(ModuleState.RETRIEVING_ORDER);
			break;
		case ORDER_CANCELLED:
			if(curState != ModuleState.RETRIEVING_ORDER && curState != ModuleState.PENDING_ORDER) {
				throw new IllegalStateException("当前状态异常：" + curState);
			}
			setState(originState);
			break;
		case ORDER_RETRY:
			if(curState != ModuleState.RETRIEVING_ORDER) {
				throw new IllegalStateException("当前状态异常：" + curState);
			}
			setState(ModuleState.PLACING_ORDER);
			break;
		default:
			throw new IllegalStateException("未有" + eventType + "事件处理逻辑");
		}
	}
	
	private void setState(ModuleState newState) {
		log.info("状态机切换：[{}] => [{}]", curState, newState);
		curState = newState;
	}

	public ModuleState getState() {
		return curState;
	}
}
