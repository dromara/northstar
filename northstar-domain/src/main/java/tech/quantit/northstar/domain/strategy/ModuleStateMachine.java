package tech.quantit.northstar.domain.strategy;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.strategy.api.StateChangeListener;
import tech.quantit.northstar.strategy.api.event.ModuleEventType;
import tech.quantit.northstar.strategy.api.log.NorthstarLoggerFactory;

/**
 * 模组状态机，管理状态转移
 * @author KevinHuangwl
 *
 */
@Deprecated
@Data
@NoArgsConstructor
public class ModuleStateMachine {

	private ModuleState curState;
	
	private ModuleState originState;
	
	private String moduleName;
	
	private List<StateChangeListener> changeListeners = new ArrayList<>();
	
	private static final String ERR_MSG = "当前状态异常：";
	
	private Logger log;
	
	public ModuleStateMachine(String moduleName, ModuleState state) {
		this.moduleName = moduleName;
		this.originState = state;
		this.curState = state;
		this.log = NorthstarLoggerFactory.getLogger(moduleName, getClass());
	}
	
	public void addStateChangeListener(StateChangeListener listener) {
		changeListeners.add(listener);
		// 设置初始状态
		listener.onChange(curState);
	}
	
	public void transformForm(ModuleEventType eventType) {
		switch(eventType) {
		case SIGNAL_CREATED:
			if(curState != ModuleState.EMPTY && !curState.isHolding()) {
				throw new IllegalStateException(ERR_MSG + curState);
			}
			originState = curState;
			setState(ModuleState.PLACING_ORDER);
			break;
		case ORDER_REQ_RETAINED:
			if(curState != ModuleState.PLACING_ORDER) {
				throw new IllegalStateException(ERR_MSG + curState);
			}
			setState(originState);
			break;
		case ORDER_CONFIRMED:
			if(curState == ModuleState.PENDING_ORDER) {
				return;
			}
			if(curState != ModuleState.PLACING_ORDER) {
				throw new IllegalStateException(ERR_MSG + curState);
			}
			setState(ModuleState.PENDING_ORDER);
			break;
		case BUY_TRADED:
			if(!curState.isOrdering()) {
				throw new IllegalStateException(ERR_MSG + curState);
			}
			setState(originState == ModuleState.EMPTY ? ModuleState.HOLDING_LONG : ModuleState.EMPTY);
			break;
		case SELL_TRADED:
			if(!curState.isOrdering()) {
				throw new IllegalStateException(ERR_MSG + curState);
			}
			setState(originState == ModuleState.EMPTY ? ModuleState.HOLDING_SHORT : ModuleState.EMPTY);
			break;
		case RETRY_RISK_ALERTED:
			if(curState != ModuleState.PENDING_ORDER) {
				throw new IllegalStateException(ERR_MSG + curState);
			}
			setState(ModuleState.RETRIEVING_FOR_RETRY);
			break;
		case REJECT_RISK_ALERTED:
			if(curState != ModuleState.PENDING_ORDER) {
				throw new IllegalStateException(ERR_MSG + curState);
			}
			break;
		case ORDER_CANCELLED:
			if(!curState.isOrdering()) {
				throw new IllegalStateException(ERR_MSG + curState);
			}
			setState(curState == ModuleState.RETRIEVING_FOR_RETRY ? ModuleState.PLACING_ORDER : originState);
			break;
		case STOP_LOSS:
			originState = curState;
			setState(ModuleState.PLACING_ORDER);
			break;
		default:
			log.debug("[{}] 事件{}不需要处理", moduleName, eventType);
		}
	}
	
	public void setState(ModuleState newState) {
		if(curState == newState)	
			return;
		log.info("[{}] 状态机切换：[{}] => [{}]", moduleName, curState, newState);
		curState = newState;
		changeListeners.stream().forEach(listener -> listener.onChange(curState));
	}

	public ModuleState getState() {
		return curState;
	}
	
}
