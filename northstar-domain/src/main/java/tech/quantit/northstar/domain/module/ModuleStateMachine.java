package tech.quantit.northstar.domain.module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.ClosingPolicy;
import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.common.utils.OrderUtils;
import tech.quantit.northstar.strategy.api.IModuleStateMachine;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 模组状态机
 * 负责计算模组状态
 * @author KevinHuangwl
 *
 */
@Slf4j
public class ModuleStateMachine implements IModuleStateMachine {
	
	private ModuleState curState = ModuleState.EMPTY;
	
	private ModuleState prevState;
	
	private String moduleName;
	
	private Map<ContractField, TradePosition> buyPosMap = new HashMap<>();
	private Map<ContractField, TradePosition> sellPosMap = new HashMap<>();
	
	public ModuleStateMachine(String moduleName) {
		this.moduleName = moduleName;
	}

	@Override
	public void onOrder(OrderField order) {
		if(curState.isOrdering() && !OrderUtils.isValidOrder(order)) {
			setState(prevState);
		}
		if(OrderUtils.isValidOrder(order) && order.getOrderStatus() != OrderStatusEnum.OS_AllTraded) {
			setState(ModuleState.PENDING_ORDER);
		}
	}

	@Override
	public void onTrade(TradeField trade) {
		if(trade.getDirection() == DirectionEnum.D_Unknown) {
			throw new IllegalArgumentException("成交方向不明确");
		}
		Map<ContractField, TradePosition> posMap = null;
		if(FieldUtils.isOpen(trade.getOffsetFlag())) {
			posMap = FieldUtils.isBuy(trade.getDirection()) ? buyPosMap : sellPosMap;
		} else {			
			posMap = FieldUtils.isBuy(trade.getDirection()) ? sellPosMap : buyPosMap;
		}
		if(posMap.containsKey(trade.getContract())) {
			posMap.get(trade.getContract()).onTrade(trade);
		} else if(FieldUtils.isOpen(trade.getOffsetFlag())) {
			posMap.put(trade.getContract(), new TradePosition(List.of(trade), ClosingPolicy.FIFO));
		}
		updateState();
	}
	
	private void updateState() {
		buyPosMap.keySet().stream().filter(c -> buyPosMap.get(c).totalVolume() == 0).forEach(c -> buyPosMap.remove(c));
		sellPosMap.keySet().stream().filter(c -> sellPosMap.get(c).totalVolume() == 0).forEach(c -> sellPosMap.remove(c));
		int buyVol = buyPosMap.values().stream().map(tp -> tp.totalVolume()).reduce(0, Integer::sum);
		int sellVol = sellPosMap.values().stream().map(tp -> tp.totalVolume()).reduce(0, Integer::sum);
		
		if(buyPosMap.isEmpty() && sellPosMap.isEmpty()) {
			setState(ModuleState.EMPTY);
		} else if(buyPosMap.isEmpty()) {
			setState(ModuleState.HOLDING_SHORT);
		} else if(sellPosMap.isEmpty()) {
			setState(ModuleState.HOLDING_LONG);
		} else if(buyPosMap.size() == sellPosMap.size() && buyPosMap.keySet().equals(sellPosMap.keySet()) && buyVol == sellVol) {
			setState(ModuleState.EMPTY_HEDGE);
		} else {
			setState(ModuleState.HOLDING_HEDGE);
		}
	}

	@Override
	public ModuleState getState() {
		return curState;
	}
	
	private void setState(ModuleState newState) {
		log.info("[{}] 状态机切换：[{}] => [{}]", moduleName, curState, newState);
		this.curState = newState;
	}

	@Override
	public void onSubmitReq(SubmitOrderReqField orderReq) {
		if(curState.isEmpty() || curState.isHolding()) {
			prevState = curState;
			setState(ModuleState.PLACING_ORDER);
		}
	}

	@Override
	public void onCancelReq(CancelOrderReqField cancelReq) {
		if(curState.isOrdering()) {
			setState(ModuleState.RETRIEVING_FOR_CANCEL);
		}
	}

}
