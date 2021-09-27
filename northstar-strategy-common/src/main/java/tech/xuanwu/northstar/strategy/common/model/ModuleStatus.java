package tech.xuanwu.northstar.strategy.common.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.utils.FieldUtils;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleDealRecord;
import tech.xuanwu.northstar.strategy.common.model.state.ModuleStateMachine;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 模组状态
 * @author KevinHuangwl
 *
 */
@Slf4j
@Data
@Builder
@AllArgsConstructor
public class ModuleStatus {

	@Id
	private String moduleName;
	
	private ModuleStateMachine stateMachine;
	
	protected Map<String, ModulePosition> longPositions;
	
	protected Map<String, ModulePosition> shortPositions;
	
	private String holdingTradingDay;
	
	private int countOfOpeningToday;
	
	private double accountAvailable;
	
	@Transient
	@Builder.Default
	private Optional<ModuleDealRecord> dealRecord = Optional.empty();
	
	// 由于lombok使用无参构造器时，默认值不会自动加上，所以要手动实现无参构造器
	public ModuleStatus() {
		this.dealRecord = Optional.empty();
	}
	
	public ModuleStatus(String name) {
		this();
		this.moduleName = name;
		this.stateMachine = new ModuleStateMachine(ModuleState.EMPTY);
		this.longPositions = new HashMap<>();
		this.shortPositions = new HashMap<>();
	}

	public double updateHoldingProfit(TickField tick) {
		if(longPositions.containsKey(tick.getUnifiedSymbol())) {
			longPositions.get(tick.getUnifiedSymbol()).updateProfit(tick);
		}
		if(shortPositions.containsKey(tick.getUnifiedSymbol())) {
			shortPositions.get(tick.getUnifiedSymbol()).updateProfit(tick);
		}
		return getHoldingProfit();
	}
	
	public Optional<SubmitOrderReqField> triggerStopLoss(TickField tick, ContractField contract){
		Optional<SubmitOrderReqField> result = Optional.empty();
		if(longPositions.containsKey(tick.getUnifiedSymbol())) {
			result = longPositions.get(tick.getUnifiedSymbol()).triggerStopLoss(tick, contract);
			if(result.isPresent()) return result;	
		}
		if(shortPositions.containsKey(tick.getUnifiedSymbol())) {
			result = shortPositions.get(tick.getUnifiedSymbol()).triggerStopLoss(tick, contract);
			if(result.isPresent()) return result;
		}
		return result;
	}
	
	public ModuleStatus onTrade(TradeField trade, OrderField order) {
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Unknown) {
			throw new IllegalStateException("未知开平仓状态");
		}
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
			opening(trade, order);
			if(StringUtils.equals(holdingTradingDay, trade.getTradingDay())) {
				countOfOpeningToday++;
			} else {
				countOfOpeningToday = 0;
				holdingTradingDay = trade.getTradingDay();
			}
		}else {
			closing(trade);
		}
		
		return this;
	}
	
	public Optional<ModuleDealRecord> consumeDealRecord(){
		Optional<ModuleDealRecord> result = dealRecord;
		dealRecord = Optional.empty();
		return result;
	}
	
	public void manuallyUpdatePosition(ModulePosition position) {
		Map<String, ModulePosition> positionMap = getPositionMap(position.getPositionDir());
		positionMap.put(position.getUnifiedSymbol(), position);
		ModuleState state = FieldUtils.isLong(position.getPositionDir()) ? ModuleState.HOLDING_LONG : ModuleState.HOLDING_SHORT;
		log.info("[{}] 手动变更模组状态：[{}]", getModuleName(), state);
		stateMachine.setCurState(state);
		stateMachine.setOriginState(state);
	}
	
	public void manuallyRemovePosition(String unifiedSymbol, PositionDirectionEnum dir) {
		Map<String, ModulePosition> positionMap = getPositionMap(dir);
		positionMap.remove(unifiedSymbol);
		log.info("[{}] 手动变更模组状态：[{}]", getModuleName(), ModuleState.EMPTY);
		stateMachine.setCurState(ModuleState.EMPTY);
		stateMachine.setOriginState(ModuleState.EMPTY);
	}
	
	public boolean at(ModuleState state) {
		return stateMachine.getState() == state;
	}
	
	public ModuleState transform(ModuleEventType event) {
		return stateMachine.transformForm(event);
	}
	
	public double getHoldingProfit() {
		double p1 = longPositions.values().stream().mapToDouble(ModulePosition::getHoldingProfit).reduce(0D, (a,b) -> a+b);
		double p2 = shortPositions.values().stream().mapToDouble(ModulePosition::getHoldingProfit).reduce(0D, (a,b) -> a+b);
		return p1 + p2;
	}
	
	public ModuleState getCurrentState() {
		return stateMachine.getState();
	}
	
	public boolean isSameDayHolding(String currentTradingDay) {
		return StringUtils.equals(currentTradingDay, holdingTradingDay);
	}
	
	private void opening(TradeField trade, OrderField order) {
		Map<String, ModulePosition> positions = trade.getDirection() == DirectionEnum.D_Buy ? longPositions : shortPositions;
		if(positions.containsKey(trade.getContract().getUnifiedSymbol())) {
			positions.get(trade.getContract().getUnifiedSymbol()).onOpenTrade(trade);
		} else {
			positions.put(trade.getContract().getUnifiedSymbol(), new ModulePosition(trade, order));
		}
		log.info("[{}] 模组开仓{}", getModuleName(), trade.getContract().getSymbol());
	}
	
	private void closing(TradeField trade) {
		Map<String, ModulePosition> positions = trade.getDirection() == DirectionEnum.D_Sell ? longPositions : shortPositions;
		if(!positions.containsKey(trade.getContract().getUnifiedSymbol())) {
			throw new IllegalStateException("不存在对应的持仓");
		}
		ModulePosition mp = positions.get(trade.getContract().getUnifiedSymbol());
		dealRecord = mp.onCloseTrade(trade);
		dealRecord.get().setModuleName(moduleName);
		if(mp.isEmpty()) {
			positions.remove(trade.getContract().getUnifiedSymbol());
			log.info("[{}] 模组平仓{}", getModuleName(), trade.getContract().getSymbol());
		}
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
}
