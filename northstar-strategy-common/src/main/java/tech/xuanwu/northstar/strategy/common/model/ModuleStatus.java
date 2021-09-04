package tech.xuanwu.northstar.strategy.common.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.EntityAware;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;
import tech.xuanwu.northstar.strategy.common.model.persistence.DealRecordPO;
import tech.xuanwu.northstar.strategy.common.model.persistence.ModulePositionPO;
import tech.xuanwu.northstar.strategy.common.model.persistence.ModuleStatusPO;
import tech.xuanwu.northstar.strategy.common.model.state.ModuleStateMachine;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
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
public class ModuleStatus implements EntityAware<ModuleStatusPO>{

	private final String moduleName;
	
	private final ModuleStateMachine stateMachine;
	
	protected final Map<String, ModulePosition> longPositions = new HashMap<>();
	
	protected final Map<String, ModulePosition> shortPositions = new HashMap<>();
	
	private final ContractManager contractMgr;
	
	private String holdingTradingDay;
	
	private int countOfOpeningToday;
	
	private double accountAvailable;
	
	private Optional<DealRecordPO> dealRecord;
	
	public ModuleStatus(String name, ContractManager contractMgr) {
		this.moduleName = name;
		this.contractMgr = contractMgr;
		this.stateMachine = new ModuleStateMachine(ModuleState.EMPTY);
	}

	public ModuleStatus(ModuleStatusPO entity, ContractManager contractMgr) {
		this.contractMgr = contractMgr;
		this.moduleName = entity.getModuleName();
		this.stateMachine = new ModuleStateMachine(entity.getState());
		this.countOfOpeningToday = entity.getCountOfOpeningToday();
		this.holdingTradingDay = entity.getHoldingTradingDay();
		entity.getPositions()
			.stream()
			.map(p -> new ModulePosition(p, contractMgr))
			.forEach(mp -> {
				if(mp.isLongPosition()) {
					longPositions.put(mp.getUnifiedSymbol(), mp);
				}
				if(mp.isShortPosition()) {
					shortPositions.put(mp.getUnifiedSymbol(), mp);
				}
			});
	}
	
	public double updateHoldingProfit(TickField tick) {
		if(longPositions.containsKey(tick.getUnifiedSymbol())) {
			longPositions.get(tick.getUnifiedSymbol()).updateProfit(tick);
		}
		if(shortPositions.containsKey(tick.getUnifiedSymbol())) {
			shortPositions.get(tick.getUnifiedSymbol()).updateProfit(tick);
		}
		double p1 = longPositions.values().stream().mapToDouble(ModulePosition::getHoldingProfit).reduce(0D, (a,b) -> a+b);
		double p2 = shortPositions.values().stream().mapToDouble(ModulePosition::getHoldingProfit).reduce(0D, (a,b) -> a+b);
		return p1 + p2;
	}
	
	public Optional<SubmitOrderReqField> triggerStopLoss(TickField tick){
		Optional<SubmitOrderReqField> result = Optional.empty();
		if(longPositions.containsKey(tick.getUnifiedSymbol())) {
			result = longPositions.get(tick.getUnifiedSymbol()).triggerStopLoss(tick);
			if(result.isPresent()) return result;
		}
		if(shortPositions.containsKey(tick.getUnifiedSymbol())) {
			result = shortPositions.get(tick.getUnifiedSymbol()).triggerStopLoss(tick);
			if(result.isPresent()) return result;
		}
		return result;
	}
	
	public Optional<ModuleStatusPO> onTrade(TradeField trade, OrderField order) {
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
		
		return Optional.of(convertToEntity());
	}
	
	public Optional<DealRecordPO> consumeDealRecord(){
		Optional<DealRecordPO> result = dealRecord;
		dealRecord = Optional.empty();
		return result;
	}
	
	
	public boolean at(ModuleState state) {
		return stateMachine.getState() == state;
	}
	
	public ModuleState transform(ModuleEventType event) {
		return stateMachine.transformForm(event);
	}
	
	public String getModuleName() {
		return moduleName;
	}
	
	public double getHoldingProfit() {
		double p1 = longPositions.values().stream().mapToDouble(ModulePosition::getHoldingProfit).reduce(0D, (a,b) -> a+b);
		double p2 = shortPositions.values().stream().mapToDouble(ModulePosition::getHoldingProfit).reduce(0D, (a,b) -> a+b);
		return p1 + p2;
	}
	
	public ModuleState getCurrentState() {
		return stateMachine.getState();
	}
	
	public int getCountOfOpeningToday() {
		return countOfOpeningToday;
	}
	
	public boolean isSameDay(String currentTradingDay) {
		return StringUtils.equals(currentTradingDay, holdingTradingDay);
	}
	
	public void setAccountAvailable(double mount) {
		this.accountAvailable = mount;
	}
	
	public double getAccountAvailable() {
		return accountAvailable;
	}
	
	private void opening(TradeField trade, OrderField order) {
		Map<String, ModulePosition> positions = trade.getDirection() == DirectionEnum.D_Buy ? longPositions : shortPositions;
		if(positions.containsKey(trade.getContract().getUnifiedSymbol())) {
			positions.get(trade.getContract().getUnifiedSymbol()).onOpenTrade(trade);
		} else {
			positions.put(trade.getContract().getUnifiedSymbol(), new ModulePosition(trade, order, contractMgr));
		}
		log.info("模组开仓{}", trade.getContract().getSymbol());
	}
	
	private void closing(TradeField trade) {
		Map<String, ModulePosition> positions = trade.getDirection() == DirectionEnum.D_Sell ? longPositions : shortPositions;
		if(!positions.containsKey(trade.getContract().getUnifiedSymbol())) {
			throw new IllegalStateException("不存在对应的持仓");
		}
		ModulePosition mp = positions.get(trade.getContract().getUnifiedSymbol());
		dealRecord = mp.onCloseTrade(trade);
		if(mp.isEmpty()) {
			positions.remove(trade.getContract().getUnifiedSymbol());
			log.info("模组平仓{}", trade.getContract().getSymbol());
		}
	}
	
	@Override
	public ModuleStatusPO convertToEntity() {
		return ModuleStatusPO.builder()
				.moduleName(moduleName)
				.state(stateMachine.getState())
				.positions(getPositionEntitys())
				.holdingTradingDay(holdingTradingDay)
				.countOfOpeningToday(countOfOpeningToday)
				.build();
	}
	
	private List<ModulePositionPO> getPositionEntitys(){
		List<ModulePositionPO> result = new ArrayList<>(longPositions.size() + shortPositions.size());
		result.addAll(longPositions.values().stream().map(ModulePosition::convertToEntity).collect(Collectors.toList()));
		result.addAll(shortPositions.values().stream().map(ModulePosition::convertToEntity).collect(Collectors.toList()));
		result.sort((a, b) -> a.getUnifiedSymbol().compareTo(b.getUnifiedSymbol()));
		return result;
	}
}
