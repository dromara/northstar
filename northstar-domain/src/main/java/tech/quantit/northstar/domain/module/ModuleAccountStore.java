package tech.quantit.northstar.domain.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.util.Assert;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.ClosingPolicy;
import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.model.ModuleAccountRuntimeDescription;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.strategy.api.IModuleAccountStore;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
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
	/* gatewayId -> commission */
	private Map<String, AtomicDouble> commissionPerDealMap = new HashMap<>();
	/* gatewayId -> initBalance*/
	private Map<String, Double> initBalanceMap = new HashMap<>();
	/* gatewayId -> accDeal */
	private Map<String, AtomicInteger> accDealVolMap = new HashMap<>();
	/* gatewayId -> accCloseProfit */
	private Map<String, AtomicDouble> accCloseProfitMap = new HashMap<>();
	
	private ModuleStateMachine sm;
	
	private ClosingPolicy closingPolicy;
	
	public ModuleAccountStore(String moduleName, ClosingPolicy closingPolicy, ModuleRuntimeDescription moduleRuntimeDescription) {
		this.sm = new ModuleStateMachine(moduleName);
		this.closingPolicy = closingPolicy;
		for(ModuleAccountRuntimeDescription mad : moduleRuntimeDescription.getAccountRuntimeDescriptionMap().values()) {
			initBalanceMap.put(mad.getAccountId(), mad.getInitBalance());
			commissionPerDealMap.put(mad.getAccountId(), new AtomicDouble(mad.getCommissionPerDeal()));
			accDealVolMap.put(mad.getAccountId(), new AtomicInteger(mad.getAccDealVolume()));
			accCloseProfitMap.put(mad.getAccountId(), new AtomicDouble(mad.getAccCloseProfit()));
			
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
	public void onOrder(OrderField order) {
		buyPositionTbl.values().stream().forEach(tp -> tp.onOrder(order));
		sellPositionTbl.values().stream().forEach(tp -> tp.onOrder(order));
		sm.onOrder(order);
	}

	@Override
	public void onTrade(TradeField trade) {
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
				accDealVolMap.putIfAbsent(trade.getGatewayId(), new AtomicInteger());
				accDealVolMap.get(trade.getGatewayId()).addAndGet(trade.getVolume());
				accCloseProfitMap.putIfAbsent(trade.getGatewayId(), new AtomicDouble());
				accCloseProfitMap.get(trade.getGatewayId()).addAndGet(profit);
			}
		} else {
			tbl.put(trade.getGatewayId(), trade.getContract().getUnifiedSymbol(), new TradePosition(List.of(trade), closingPolicy));
		}
		sm.onTrade(trade);
	}
	
	/* 更新持仓盈亏 */
	@Override
	public void onTick(TickField tick) {
		buyPositionTbl.values().stream().forEach(tp -> tp.updateTick(tick));
		sellPositionTbl.values().stream().forEach(tp -> tp.updateTick(tick));
	}

	@Override
	public double getInitBalance(String gatewayId) {
		if(!initBalanceMap.containsKey(gatewayId)) {
			throw new NoSuchElementException("找不到网关余额：" + gatewayId);
		}
		return initBalanceMap.get(gatewayId);
	}
	
	private double getCommission(String gatewayId) {
		if(!commissionPerDealMap.containsKey(gatewayId)) {
			throw new NoSuchElementException("找不到网关手续费：" + gatewayId);
		}
		return commissionPerDealMap.get(gatewayId).get();
	}

	@Override
	public double getPreBalance(String gatewayId) {
		return getInitBalance(gatewayId) + getAccCloseProfit(gatewayId) - getCommission(gatewayId) * getAccDealVolume(gatewayId);
	}

	@Override
	public List<TradeField> getUncloseTrades(String gatewayId) {
		Collection<TradePosition> buyOpenPositions = buyPositionTbl.row(gatewayId).values();
		Collection<TradePosition> sellOpenPositions = sellPositionTbl.row(gatewayId).values();
		List<TradeField> resultList = new ArrayList<>();
		buyOpenPositions.stream().forEach(tp -> resultList.addAll(tp.getUncloseTrades()));
		sellOpenPositions.stream().forEach(tp -> resultList.addAll(tp.getUncloseTrades()));
		return resultList;
	}
	
	@Override
	public int getAccDealVolume(String gatewayId) {
		if(!accDealVolMap.containsKey(gatewayId)) {
			throw new NoSuchElementException("找不到网关交易数：" + gatewayId);
		}
		return accDealVolMap.get(gatewayId).get();
	}

	@Override
	public double getAccCloseProfit(String gatewayId) {
		if(!accCloseProfitMap.containsKey(gatewayId)) {
			throw new NoSuchElementException("找不到网关平仓盈亏：" + gatewayId);
		}
		return accCloseProfitMap.get(gatewayId).get();
	}

	@Override
	public void tradeDayPreset() {
		buyPositionTbl.values().stream().forEach(TradePosition::releaseOrders);
		sellPositionTbl.values().stream().forEach(TradePosition::releaseOrders);
	}

	@Override
	public List<PositionField> getPositions(String gatewayId) {
		List<PositionField> positionList = new ArrayList<>();
		positionList.addAll(buyPositionTbl.values().stream().map(TradePosition::convertToPositionField).toList());
		positionList.addAll(sellPositionTbl.values().stream().map(TradePosition::convertToPositionField).toList());
		return positionList;
	}

	@Override
	public ModuleState getModuleState() {
		return sm.getState();
	}

	@Override
	public void onSubmitOrder(SubmitOrderReqField submitOrder) {
		sm.onSubmitReq(submitOrder);
	}

	@Override
	public void onCancelOrder(CancelOrderReqField cancelOrder) {
		sm.onCancelReq(cancelOrder);
	}

}
