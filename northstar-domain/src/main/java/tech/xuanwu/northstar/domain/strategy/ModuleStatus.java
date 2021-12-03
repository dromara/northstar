package tech.xuanwu.northstar.domain.strategy;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.utils.FieldUtils;
import tech.xuanwu.northstar.strategy.api.constant.ModuleState;

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
	private ModuleStateMachine stateMachine;
	
	@Getter
	protected ModulePosition longPosition;
	
	@Getter
	protected ModulePosition shortPosition;
	
	public ModuleStatus(String name) {
		this.moduleName = name;
		this.stateMachine = new ModuleStateMachine(name, ModuleState.EMPTY);
	}
	
	/**
	 * 增加持仓
	 * @param position
	 */
	public void addPosition(ModulePosition position) {
		log.info("[{}] 持仓增加，{} {} {}", getModuleName(), position.contract().getUnifiedSymbol(), position.getDirection(), position.getVolume());
		if(FieldUtils.isLong(position.getDirection())) {
			longPosition = position;
		}
		if(FieldUtils.isShort(position.getDirection())) {
			shortPosition = position;
		}
		
		ModuleState state = getMergedState();
		int longVol = longPosition == null ? 0 : longPosition.getVolume();
		int shortVol = shortPosition == null ? 0 : shortPosition.getVolume();
		log.info("[{}] 变更模组状态：[{}]，多头{}手，空头{}手", getModuleName(), state, longVol, shortVol);
		stateMachine.setCurState(state);
		stateMachine.setOriginState(state);
	}
	
	/**
	 * 移除持仓
	 * @param position
	 */
	public void removePostion(ModulePosition position) {
		log.info("[{}] 移除持仓，{} {} {}", getModuleName(), position.contract().getUnifiedSymbol(), position.getDirection());
		if(FieldUtils.isLong(position.getDirection())) {
			longPosition = null;
		}
		if(FieldUtils.isShort(position.getDirection())) {
			shortPosition = null;
		}
		ModuleState state = getMergedState();
		log.info("[{}] 变更模组状态：[{}]", getModuleName(), state);
		stateMachine.setCurState(state);
		stateMachine.setOriginState(state);
	}
	
	public boolean at(ModuleState state) {
		return stateMachine.getState() == state;
	}
	
	public double holdingProfit() {
		double p1 = longPosition != null ? longPosition.getProfit() : 0;
		double p2 = shortPosition != null ? shortPosition.getProfit() : 0;
		return p1 + p2;
	}
	
	private ModuleState getMergedState() {
		if(longPosition == shortPosition)
			return ModuleState.EMPTY;
		if(longPosition == null)
			return ModuleState.HOLDING_SHORT;
		if(shortPosition == null)
			return ModuleState.HOLDING_LONG;
		if(longPosition.getVolume() > shortPosition.getVolume())
			return ModuleState.HOLDING_LONG;
		if(longPosition.getVolume() < shortPosition.getVolume())
			return ModuleState.HOLDING_SHORT;
		return ModuleState.NET_EMPTY;
	}
	
	public List<ModulePosition> getAllPositions(){
		List<ModulePosition> resultList = new ArrayList<>(2);
		if(longPosition != null) 
			resultList.add(longPosition);
		if(shortPosition != null)
			resultList.add(shortPosition);
		return resultList;
	}
}
