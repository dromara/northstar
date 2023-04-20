package org.dromara.northstar.module.legacy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.module.IModuleStateMachine;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
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
		if(!curState.isOrdering()) {
			throw new IllegalStateException("当前状态异常：" + curState);
		}
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
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Unknown) {
			throw new IllegalArgumentException("操作意图不明确");
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
		buyPosMap = buyPosMap.entrySet().stream().filter(e -> e.getValue().totalVolume() > 0).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		sellPosMap = sellPosMap.entrySet().stream().filter(e -> e.getValue().totalVolume() > 0).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		int buyVol = buyPosMap.values().stream().map(TradePosition::totalVolume).reduce(0, Integer::sum);
		int sellVol = sellPosMap.values().stream().map(TradePosition::totalVolume).reduce(0, Integer::sum);
		
		if(isEmpty(buyPosMap) && isEmpty(sellPosMap)) {
			setState(ModuleState.EMPTY);
		} else if(isEmpty(buyPosMap)) {
			setState(ModuleState.HOLDING_SHORT);
		} else if(isEmpty(sellPosMap)) {
			setState(ModuleState.HOLDING_LONG);
		} else if(buyPosMap.size() == sellPosMap.size() && buyPosMap.keySet().equals(sellPosMap.keySet()) && buyVol == sellVol) {
			setState(ModuleState.EMPTY_HEDGE);
		} else {
			setState(ModuleState.HOLDING_HEDGE);
		}
	}
	
	private boolean isEmpty(Map<ContractField, TradePosition> posMap) {
		int pos = posMap.values().stream().mapToInt(TradePosition::totalVolume).sum();
		return posMap.isEmpty() || pos == 0;
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
		if(curState.isOrdering()) {
			throw new IllegalStateException("当前状态异常：" + curState);
		}
		prevState = curState;
		setState(ModuleState.PLACING_ORDER);
	}

	@Override
	public void onCancelReq(CancelOrderReqField cancelReq) {
		if(!curState.isOrdering()) {
			throw new IllegalStateException("当前状态异常：" + curState);
		}
		setState(ModuleState.RETRIEVING_FOR_CANCEL);
	}

}
