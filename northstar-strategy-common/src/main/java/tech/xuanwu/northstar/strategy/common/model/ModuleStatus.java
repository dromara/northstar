package tech.xuanwu.northstar.strategy.common.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import tech.xuanwu.northstar.common.EntityAware;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;
import tech.xuanwu.northstar.strategy.common.model.entity.ModulePositionEntity;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleStatusEntity;
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
public class ModuleStatus implements EntityAware<ModuleStatusEntity>{

	protected String moduleName;
	
	protected double accountAvailable;
	
	protected ModuleStateMachine stateMachine;
	
	protected List<ModulePosition> positions;
	
	protected ContractManager contractMgr;
	
	protected String holdingTradingDay;
	
	protected int countOfOpeningToday;
	
	public ModuleStatus(String name, ContractManager contractMgr) {
		this.moduleName = name;
		this.contractMgr = contractMgr;
		this.stateMachine = new ModuleStateMachine(ModuleState.EMPTY);
		this.positions = new ArrayList<>();
	}

	public ModuleStatus(ModuleStatusEntity entity, ContractManager contractMgr) {
		this.contractMgr = contractMgr;
		this.moduleName = entity.getModuleName();
		this.stateMachine = new ModuleStateMachine(entity.getState());
		this.countOfOpeningToday = entity.getCountOfOpeningToday();
		this.holdingTradingDay = entity.getHoldingTradingDay();
		this.positions = Lists.newArrayList(entity.getPositions().stream().map(ModulePosition::new).collect(Collectors.toList()));
	}
	
	public double updateHoldingProfit(TickField tick) {
		return positions.stream()
				.filter(p -> p.isMatch(tick.getUnifiedSymbol()))
				.map(p -> p.updateProfit(tick))
				.reduce(0D, (a, b) -> a + b);
	}
	
	public Optional<SubmitOrderReqField> triggerStopLoss(TickField tick){
		for(ModulePosition p : positions) {
			if(!p.isMatch(tick.getUnifiedSymbol())) {
				continue;
			}
			if(p.triggerStopLoss(tick)) {
				ContractField contract = contractMgr.getContract(tick.getUnifiedSymbol());
				return Optional.of(SubmitOrderReqField.newBuilder()
						.setOriginOrderId(UUID.randomUUID().toString())
						.setContract(contract)
						.build());
			}
		}
		return Optional.empty();
	}
	
	public ModuleStatus onTrade(TradeField trade, OrderField order) {
		if(!StringUtils.equals(trade.getOriginOrderId(), order.getOriginOrderId())) {
			throw new IllegalArgumentException("传入的成交与订单不匹配");
		}
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
		return positions.stream()
				.map(p -> p.holdingProfit)
				.reduce(0D, (a, b) -> a + b);
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
		if(positions.stream().filter(p -> p.isMatch(trade.getContract().getUnifiedSymbol())).anyMatch(p -> p.onTrade(trade))) {
			return;
		}
		ModulePositionEntity e = ModulePositionEntity.builder()
				.openPrice(trade.getPrice())
				.stopLossPrice(order.getStopPrice())
				.positionDir(trade.getDirection() == DirectionEnum.D_Buy ? PositionDirectionEnum.PD_Long : PositionDirectionEnum.PD_Short)
				.unifiedSymbol(trade.getContract().getUnifiedSymbol())
				.volume(trade.getVolume())
				.multiplier(trade.getContract().getMultiplier())
				.build();
		positions.add(new ModulePosition(e));
	}
	
	private void closing(TradeField trade) {
		if(positions.stream().filter(p -> p.isMatch(trade.getContract().getUnifiedSymbol())).anyMatch(e -> e.onTrade(trade))) {
			Iterator<ModulePosition> itPos = positions.iterator();
			while(itPos.hasNext()) {
				if(itPos.next().isEmpty()) {					
					itPos.remove();
				}
			}
		}
	}
	
	@Override
	public ModuleStatusEntity convertToEntity() {
		return ModuleStatusEntity.builder()
				.moduleName(moduleName)
				.state(stateMachine.getState())
				.positions(positions.stream().map(ModulePosition::convertToEntity).collect(Collectors.toList()))
				.holdingTradingDay(holdingTradingDay)
				.build();
	}
}
