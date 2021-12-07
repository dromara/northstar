package tech.quantit.northstar.domain.strategy;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.strategy.api.constant.ModuleState;
import tech.quantit.northstar.strategy.api.event.ModuleEventBus;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * 模组状态
 * 模组持仓理论上只能支持一个合约的单向持仓或双向持仓；(暂不考虑套利模型)
 * @author KevinHuangwl
 *
 */
@Slf4j
@Data
public class ModuleStatus {

	@Getter
	private String moduleName;
	
	@Getter
	protected ModuleStateMachine stateMachine;
	
	// 模组的逻辑净持仓
	@Getter
	protected ModulePosition logicalPosition;
	
	@Setter
	private ModuleEventBus moduleEventBus;
	
	public ModuleStatus(String name) {
		this.moduleName = name;
		this.stateMachine = new ModuleStateMachine(name, ModuleState.EMPTY);
	}
	
	/**
	 * 更新持仓
	 * @param position
	 */
	public void updatePosition(ModulePosition position) {
		log.info("[{}] 持仓更新，{} {} {}", getModuleName(), position.contract().getUnifiedSymbol(), position.getDirection(), position.getVolume());
		position.setEventBus(moduleEventBus);
		if(logicalPosition == null) {
			logicalPosition = position;
		} else {
			logicalPosition.merge(position);
		}
		
		ModuleState state = getMergedState();
		stateMachine.setState(state);
		
		log.info("[{}] 变更模组状态：[{}]，{}手", getModuleName(), state, logicalPosition == null ? 0 : logicalPosition.getVolume());
	}
	
	/**
	 * 移除持仓
	 * @param unifiedSymbol
	 * @param dir
	 */
	public void removePosition(String unifiedSymbol, PositionDirectionEnum dir) {
		log.info("[{}] 移除持仓，{} {}", getModuleName(), unifiedSymbol, dir);
		logicalPosition = null;
		ModuleState state = getMergedState();
		stateMachine.setState(state);
		log.info("[{}] 变更模组状态：[{}]", getModuleName(), state);
	}
	
	public boolean at(ModuleState state) {
		return stateMachine.getState() == state;
	}
	
	public double holdingProfit() {
		return logicalPosition.getProfit();
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
	
}
