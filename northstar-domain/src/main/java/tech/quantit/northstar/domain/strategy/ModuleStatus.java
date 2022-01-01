package tech.quantit.northstar.domain.strategy;

import org.slf4j.Logger;

import lombok.Getter;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.strategy.api.constant.ModuleState;
import tech.quantit.northstar.strategy.api.event.ModuleEventBus;
import tech.quantit.northstar.strategy.api.log.NorthstarLoggerFactory;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 模组状态
 * 模组持仓理论上只能支持一个合约的单向持仓或双向持仓；(暂不考虑套利模型)
 * @author KevinHuangwl
 *
 */
public class ModuleStatus {

	@Getter
	private String moduleName;
	
	@Getter
	protected ModuleStateMachine stateMachine;
	
	// 模组的逻辑净持仓
	@Getter
	protected ModulePosition logicalPosition;
	
	@Getter
	private ModuleEventBus moduleEventBus;
	
	private Logger log;
	
	
	public ModuleStatus(String name, ModulePosition modulePosition) {
		moduleName = name;
		log = NorthstarLoggerFactory.getLogger(name, getClass());
		logicalPosition = modulePosition;
		stateMachine = new ModuleStateMachine(name, ModuleState.EMPTY);
		stateMachine.setState(getMergedState());
	}
	
	/**
	 * 更新持仓
	 * @param position
	 */
	public void updatePosition(TradeField trade, SubmitOrderReqField orderReq) {
		logicalPosition.merge(trade, orderReq.getStopPrice());
		ModuleState state = getMergedState();
		stateMachine.setState(state);
		
		log.info("[{}] 变更模组状态：[{}]，{}手", getModuleName(), state, logicalPosition.getVolume());
	}
	
	/**
	 * 移除持仓
	 * @param unifiedSymbol
	 * @param dir
	 */
	public void removePosition() {
		log.info("[{}] 移除持仓", getModuleName());
		logicalPosition.clearout();
		ModuleState state = getMergedState();
		stateMachine.setState(state);
		log.info("[{}] 变更模组状态：[{}]", getModuleName(), state);
	}
	
	public boolean at(ModuleState state) {
		return stateMachine.getState() == state;
	}
	
	public double holdingProfit() {
		return logicalPosition.profit();
	}
	
	private ModuleState getMergedState() {
		if(logicalPosition == null || logicalPosition.getVolume() == 0)
			return ModuleState.EMPTY;
		if(FieldUtils.isLong(logicalPosition.getDirection()))
			return ModuleState.HOLDING_LONG;
		if(FieldUtils.isShort(logicalPosition.getDirection()))
			return ModuleState.HOLDING_SHORT;
		throw new IllegalStateException("未知状态");
	}

	public void setModuleEventBus(ModuleEventBus moduleEventBus) {
		this.moduleEventBus = moduleEventBus;
		logicalPosition.setEventBus(moduleEventBus);
		moduleEventBus.register(logicalPosition);
	}

}
