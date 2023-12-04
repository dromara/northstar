package org.dromara.northstar.gateway.sim.trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;

/**
 * 管理持仓
 * @author KevinHuangwl
 *
 */
public class PositionManager implements TransactionAware, TickDataAware {
	
	private SimGatewayAccount account;
	
	/* unifiedSymbol -> tick */
	private ConcurrentMap<Contract, TradePosition> buyPosMap = new ConcurrentHashMap<>();
	private ConcurrentMap<Contract, TradePosition> sellPosMap = new ConcurrentHashMap<>();
	
	public PositionManager(SimGatewayAccount account) {
		this.account = account;
	}
	
	public PositionManager(SimGatewayAccount account, List<Trade> nonclosedTrade) {
		this.account = account;
		nonclosedTrade.forEach(this::onTrade);
	}

	@Override
	public void onTick(Tick tick) {
		buyPosMap.values().forEach(tp -> tp.onTick(tick));
		sellPosMap.values().forEach(tp -> tp.onTick(tick));
	}

	@Override
	public void onOrder(Order order) {
		if(FieldUtils.isClose(order.offsetFlag())) {
			TradePosition pos = getPosition(order.direction(), order.contract(), true);
			if(Objects.isNull(pos)) {
				throw new NoSuchElementException(String.format("找不到%s头持仓：%s", FieldUtils.chn(order.direction()), order.contract().unifiedSymbol()));
			}
			pos.onOrder(order);
		}
	}

	@Override
	public void onTrade(Trade trade) {
		DirectionEnum dir = trade.direction();
		if(FieldUtils.isOpen(trade.offsetFlag())) {
			TradePosition tp = getPosition(dir, trade.contract(), false);
			if(Objects.isNull(tp)) {
				tp = new TradePosition(trade.contract(), dir);
				getPosMap(dir, false).put(trade.contract(), tp);
			}
			tp.onTrade(trade);
		} else {
			TradePosition tp = getPosition(dir, trade.contract(), true);
			if(Objects.isNull(tp)) {
				throw new NoSuchElementException(String.format("找不到%s头持仓：%s", FieldUtils.chn(dir), trade.contract().unifiedSymbol()));
			}
			tp.onTrade(trade).forEach(account::onDeal);
		}
	}
	
	public List<Position> positionFields() {
		List<Position> resultList = new ArrayList<>();
		resultList.addAll(buyPosMap.values().stream().map(tp -> tp.convertToPosition(account.getAccountDescription().getGatewayId())).toList());
		resultList.addAll(sellPosMap.values().stream().map(tp -> tp.convertToPosition(account.getAccountDescription().getGatewayId())).toList());
		return resultList;
	}
	
	public List<Trade> getNonclosedTrade() {
		List<Trade> resultList = new ArrayList<>();
		resultList.addAll(buyPosMap.values().stream().flatMap(tp -> tp.getUncloseTrades().stream()).toList());
		resultList.addAll(sellPosMap.values().stream().flatMap(tp -> tp.getUncloseTrades().stream()).toList());
		return resultList;
	}
	
	public double totalHoldingProfit() {
		return positionFields().stream().mapToDouble(Position::positionProfit).sum();
	}
	
	public double totalMargin() {
		return positionFields().stream().mapToDouble(Position::exchangeMargin).sum();
	}

	public int getAvailablePosition(DirectionEnum direction, Contract contract) {
		TradePosition tp = getPosition(direction, contract, false);
		if(Objects.isNull(tp)) {
			throw new NoSuchElementException(String.format("找不到%s头持仓：%s", FieldUtils.chn(direction), contract.unifiedSymbol()));
		}		
		return tp.totalAvailable();
	}
	
	private TradePosition getPosition(DirectionEnum dir, Contract contract, boolean reverse){
		return getPosMap(dir, reverse).get(contract);
	}
	
	private ConcurrentMap<Contract, TradePosition> getPosMap(DirectionEnum dir, boolean reverse){
		return switch(dir) {
		case D_Buy -> reverse ? sellPosMap : buyPosMap;
		case D_Sell -> reverse ? buyPosMap : sellPosMap;
		default -> throw new IllegalArgumentException("Unexpected value: " + dir);
		};
	}
}
