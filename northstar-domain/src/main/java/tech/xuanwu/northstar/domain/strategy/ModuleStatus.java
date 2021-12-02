package tech.xuanwu.northstar.domain.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.utils.FieldUtils;
import tech.xuanwu.northstar.strategy.api.constant.ModuleState;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * 模组状态
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
	
	protected ConcurrentMap<String, ModulePosition> longPositions;
	
	protected ConcurrentMap<String, ModulePosition> shortPositions;
	
	public ModuleStatus(String name) {
		this.moduleName = name;
		this.stateMachine = new ModuleStateMachine(name, ModuleState.EMPTY);
		this.longPositions = new ConcurrentHashMap<>();
		this.shortPositions = new ConcurrentHashMap<>();
	}
	
	public void addPosition(ModulePosition position) {
		log.info("[{}] 加入持仓，{} {} {}", getModuleName(), position.contract().getUnifiedSymbol(), position.getDirection(), position.getVolume());
		Map<String, ModulePosition> positionMap = getPositionMap(position.getDirection());
		positionMap.put(position.contract().getUnifiedSymbol(), position);
		if(at(ModuleState.EMPTY)) {			
			ModuleState state = FieldUtils.isLong(position.getDirection()) ? ModuleState.HOLDING_LONG : ModuleState.HOLDING_SHORT;
			log.info("[{}] 变更模组状态：[{}]", getModuleName(), state);
			stateMachine.setCurState(state);
			stateMachine.setOriginState(state);
		}
	}
	
	public void removePostion(ModulePosition position) {
		removePosition(position.contract().getUnifiedSymbol(), position.getDirection());
	}
	
	public void removePosition(String unifiedSymbol, PositionDirectionEnum dir) {
		log.info("[{}] 移除持仓，{} {}", getModuleName(), unifiedSymbol, dir);
		Map<String, ModulePosition> positionMap = getPositionMap(dir);
		positionMap.remove(unifiedSymbol);
		if(at(ModuleState.HOLDING_LONG) || at(ModuleState.HOLDING_SHORT)) {			
			log.info("[{}] 变更模组状态：[{}]", getModuleName(), ModuleState.EMPTY);
			stateMachine.setCurState(ModuleState.EMPTY);
			stateMachine.setOriginState(ModuleState.EMPTY);
		}
	}
	
	public boolean at(ModuleState state) {
		return stateMachine.getState() == state;
	}
	
	private Map<String, ModulePosition> getPositionMap(PositionDirectionEnum dir){
		if(dir == PositionDirectionEnum.PD_Long) {
			return longPositions;
		}
		if(dir == PositionDirectionEnum.PD_Short) {
			return shortPositions;
		}
		throw new IllegalArgumentException("非法持仓方向：" + dir);
	}
	
	public double holdingProfit() {
		double p1 = longPositions.values().stream().mapToDouble(ModulePosition::getProfit).reduce(0D, (a,b) -> a+b);
		double p2 = shortPositions.values().stream().mapToDouble(ModulePosition::getProfit).reduce(0D, (a,b) -> a+b);
		return p1 + p2;
	}
	
	public ModuleState state() {
		return stateMachine.getCurState();
	}

	public List<ModulePosition> getAllPositions(){
		List<ModulePosition> resultList = new ArrayList<>(longPositions.size() + shortPositions.size());
		resultList.addAll(longPositions.values());
		resultList.addAll(shortPositions.values());
		return resultList;
	}
}
