package org.dromara.northstar.domain.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleAccountRuntimeDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.gateway.api.IContractManager;
import org.dromara.northstar.gateway.api.domain.contract.Contract;
import org.dromara.northstar.strategy.api.IModuleAccountStore;
import org.springframework.util.Assert;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 模组账户状态存储器
 * @author KevinHuangwl
 *
 */
@Slf4j
public class ModuleAccountStore implements IModuleAccountStore {

	/* gatewayId -> unifiedSymbol -> position */
	private Table<String, String, TradePosition> buyPositionTbl = HashBasedTable.create();
	private Table<String, String, TradePosition> sellPositionTbl = HashBasedTable.create();
	/* gatewayId -> accCommission */
	private Map<String, AtomicDouble> accCommissionMap = new HashMap<>();	// 使用Atomic是方便累加操作
	/* gatewayId -> initBalance*/
	private Map<String, Double> initBalanceMap = new HashMap<>();
	/* gatewayId -> accDeal */
	private Map<String, AtomicInteger> accDealVolMap = new HashMap<>();		// 使用Atomic是方便累加操作
	/* gatewayId -> accCloseProfit */
	private Map<String, AtomicDouble> accCloseProfitMap = new HashMap<>();	// 使用Atomic是方便累加操作
	/* gatewayId -> maxProfit */
	private Map<String, Double> maxProfitMap = new HashMap<>();
	/* gatewayId -> maxDrawBack */
	private Map<String, Double> maxDrawBackMap = new HashMap<>();
	
	private ModuleStateMachine sm;
	
	private ClosingPolicy closingPolicy;
	
	private IContractManager contractMgr;
	
	public ModuleAccountStore(String moduleName, ClosingPolicy closingPolicy, ModuleRuntimeDescription moduleRuntimeDescription,
			IContractManager contractMgr) {
		this.sm = new ModuleStateMachine(moduleName);
		this.closingPolicy = closingPolicy;
		this.contractMgr = contractMgr;
		for(ModuleAccountRuntimeDescription mad : moduleRuntimeDescription.getAccountRuntimeDescriptionMap().values()) {
			initBalanceMap.put(mad.getAccountId(), mad.getInitBalance());
			accDealVolMap.put(mad.getAccountId(), new AtomicInteger(mad.getAccDealVolume()));
			accCloseProfitMap.put(mad.getAccountId(), new AtomicDouble(mad.getAccCloseProfit()));
			accCommissionMap.put(mad.getAccountId(), new AtomicDouble(mad.getAccCommission()));
			
			List<TradeField> allTrades = mad.getPositionDescription()
					.getUncloseTrades()
					.stream()
					.map(this::parseFrom)
					.filter(Objects::nonNull)
					.toList();
			
			for(TradeField trade : allTrades) {
				Assert.isTrue(trade.getDirection() != DirectionEnum.D_Unknown, "成交方向不正确");
				String gatewayId = trade.getGatewayId();
				String unifiedSymbol = trade.getContract().getUnifiedSymbol();
				Table<String, String, TradePosition> tbl = trade.getDirection() == DirectionEnum.D_Buy ? buyPositionTbl : sellPositionTbl;
				if(tbl.contains(gatewayId, unifiedSymbol)) {
					tbl.get(gatewayId, unifiedSymbol).onTrade(trade);
				} else {
					tbl.put(gatewayId, unifiedSymbol, new TradePosition(List.of(trade), closingPolicy));
				}
				sm.onTrade(trade);
			}
		}
	}
	
	private TradeField parseFrom(byte[] data) {
		try {
			return TradeField.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			log.warn("", e);
			return null;
		}
	}
	
	/* 更新 */
	@Override
	public synchronized void onOrder(OrderField order) {
		buyPositionTbl.values().stream().forEach(tp -> tp.onOrder(order));
		sellPositionTbl.values().stream().forEach(tp -> tp.onOrder(order));
		sm.onOrder(order);
	}

	@Override
	public synchronized void onTrade(TradeField trade) {
		Table<String, String, TradePosition> tbl = null;
		if(FieldUtils.isOpen(trade.getOffsetFlag())) {
			tbl = FieldUtils.isBuy(trade.getDirection()) ? buyPositionTbl : sellPositionTbl;
		} else {
			tbl = FieldUtils.isBuy(trade.getDirection()) ? sellPositionTbl : buyPositionTbl;
		}
		if(tbl.contains(trade.getGatewayId(), trade.getContract().getUnifiedSymbol())) {
			TradePosition tp = tbl.get(trade.getGatewayId(), trade.getContract().getUnifiedSymbol());
			double profit = tp.onTrade(trade);
			if(FieldUtils.isClose(trade.getOffsetFlag())) {
				String gatewayId = trade.getGatewayId();
				accDealVolMap.putIfAbsent(gatewayId, new AtomicInteger());
				accCloseProfitMap.putIfAbsent(gatewayId, new AtomicDouble());
				accCommissionMap.putIfAbsent(gatewayId, new AtomicDouble());
				maxProfitMap.putIfAbsent(gatewayId, 0D);
				maxDrawBackMap.putIfAbsent(gatewayId, 0D);
				
				accDealVolMap.get(gatewayId).addAndGet(trade.getVolume());
				accCloseProfitMap.get(gatewayId).addAndGet(profit);
				Contract contract = contractMgr.getContract(Identifier.of(trade.getContract().getContractId()));
				ContractField cf = contract.contractField();
				double commission = cf.getCommissionFee() > 0 ? cf.getCommissionFee() : cf.getCommissionRate() * trade.getPrice() * trade.getContract().getMultiplier();
				accCommissionMap.get(gatewayId).addAndGet(commission * 2 * trade.getVolume()); // 乘2代表手续费双向计算
				double maxProfit = Math.max(getMaxProfit(gatewayId), getAccCloseProfit(gatewayId) - getAccCommission(gatewayId));
				maxProfitMap.put(gatewayId, maxProfit);
				double maxDrawBack = Math.min(getMaxDrawBack(gatewayId), getAccCloseProfit(gatewayId) - getAccCommission(gatewayId) - maxProfit);
				maxDrawBackMap.put(gatewayId, maxDrawBack);
			}
		} else if(FieldUtils.isOpen(trade.getOffsetFlag())) {
			tbl.put(trade.getGatewayId(), trade.getContract().getUnifiedSymbol(), new TradePosition(List.of(trade), closingPolicy));
		}
		sm.onTrade(trade);
	}
	
	/* 更新持仓盈亏 */
	@Override
	public synchronized void onTick(TickField tick) {
		buyPositionTbl.values().stream().forEach(tp -> tp.updateTick(tick));
		sellPositionTbl.values().stream().forEach(tp -> tp.updateTick(tick));
	}

	@Override
	public synchronized double getInitBalance(String gatewayId) {
		if(!initBalanceMap.containsKey(gatewayId)) {
			throw new NoSuchElementException("找不到网关余额：" + gatewayId);
		}
		return initBalanceMap.get(gatewayId);
	}
	
	@Override
	public synchronized double getPreBalance(String gatewayId) {
		return getInitBalance(gatewayId) + getAccCloseProfit(gatewayId) - getAccCommission(gatewayId);
	}

	@Override
	public synchronized List<TradeField> getUncloseTrades(String gatewayId) {
		Collection<TradePosition> buyOpenPositions = buyPositionTbl.row(gatewayId).values();
		Collection<TradePosition> sellOpenPositions = sellPositionTbl.row(gatewayId).values();
		List<TradeField> resultList = new ArrayList<>();
		buyOpenPositions.stream().forEach(tp -> resultList.addAll(tp.getUncloseTrades()));
		sellOpenPositions.stream().forEach(tp -> resultList.addAll(tp.getUncloseTrades()));
		return resultList;
	}
	
	@Override
	public synchronized int getAccDealVolume(String gatewayId) {
		if(!accDealVolMap.containsKey(gatewayId)) {
			throw new NoSuchElementException("找不到网关交易数：" + gatewayId);
		}
		return accDealVolMap.get(gatewayId).get();
	}

	@Override
	public synchronized double getAccCloseProfit(String gatewayId) {
		if(!accCloseProfitMap.containsKey(gatewayId)) {
			throw new NoSuchElementException("找不到网关平仓盈亏：" + gatewayId);
		}
		return accCloseProfitMap.get(gatewayId).get();
	}

	@Override
	public synchronized void tradeDayPreset() {
		buyPositionTbl.values().stream().forEach(TradePosition::releaseOrders);
		sellPositionTbl.values().stream().forEach(TradePosition::releaseOrders);
	}

	@Override
	public synchronized List<PositionField> getPositions(String gatewayId) {
		List<PositionField> positionList = new ArrayList<>();
		positionList.addAll(buyPositionTbl.row(gatewayId).values().stream().map(TradePosition::convertToPositionField).toList());
		positionList.addAll(sellPositionTbl.row(gatewayId).values().stream().map(TradePosition::convertToPositionField).toList());
		return positionList;
	}
	
	@Override
	public ModuleState getModuleState() {
		return sm.getState();
	}

	@Override
	public synchronized void onSubmitOrder(SubmitOrderReqField submitOrder) {
		sm.onSubmitReq(submitOrder);
	}

	@Override
	public synchronized void onCancelOrder(CancelOrderReqField cancelOrder) {
		sm.onCancelReq(cancelOrder);
	}

	@Override
	public synchronized double getAccCommission(String gatewayId) {
		return accCommissionMap.get(gatewayId).get();
	}

	@Override
	public synchronized double getMaxDrawBack(String gatewayId) {
		return Optional.ofNullable(maxDrawBackMap.get(gatewayId)).orElse(0D);
	}

	@Override
	public synchronized double getMaxProfit(String gatewayId) {
		return Optional.ofNullable(maxProfitMap.get(gatewayId)).orElse(0D);
	}

	@Override
	public String toString() {
		return "ModuleAccountStore [buyPositionTbl=" + buyPositionTbl + ", sellPositionTbl=" + sellPositionTbl + "]";
	}
	
}
