package org.dromara.northstar.gateway.sim.trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.utils.FieldUtils;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 管理持仓
 * @author KevinHuangwl
 *
 */
public class PositionManager implements TransactionAware, TickDataAware {
	
	private SimGatewayAccount account;
	
	/* unifiedSymbol -> tick */
	private ConcurrentMap<String, TradePosition> buyPosMap = new ConcurrentHashMap<>();
	private ConcurrentMap<String, TradePosition> sellPosMap = new ConcurrentHashMap<>();
	
	public PositionManager(SimGatewayAccount account) {
		this.account = account;
	}
	
	public PositionManager(SimGatewayAccount account, List<TradeField> nonclosedTrade) {
		this.account = account;
		nonclosedTrade.forEach(this::onTrade);
	}

	@Override
	public void onTick(TickField tick) {
		buyPosMap.values().forEach(tp -> tp.onTick(tick));
		sellPosMap.values().forEach(tp -> tp.onTick(tick));
	}

	@Override
	public void onOrder(OrderField order) {
		if(FieldUtils.isClose(order.getOffsetFlag())) {
			TradePosition pos = getPosition(order.getDirection(), order.getContract().getUnifiedSymbol(), true);
			if(Objects.isNull(pos)) {
				throw new NoSuchElementException(String.format("找不到%s头持仓：%s", FieldUtils.chn(order.getDirection()), order.getContract().getUnifiedSymbol()));
			}
			pos.onOrder(order);
		}
	}

	@Override
	public void onTrade(TradeField trade) {
		DirectionEnum dir = trade.getDirection();
		String unifiedSymbol = trade.getContract().getUnifiedSymbol();
		if(FieldUtils.isOpen(trade.getOffsetFlag())) {
			TradePosition tp = getPosition(dir, unifiedSymbol, false);
			if(Objects.isNull(tp)) {
				tp = new TradePosition(trade.getContract(), dir);
				getPosMap(dir, false).put(unifiedSymbol, tp);
			}
			tp.onTrade(trade);
		} else {
			TradePosition tp = getPosition(dir, unifiedSymbol, true);
			if(Objects.isNull(tp)) {
				throw new NoSuchElementException(String.format("找不到%s头持仓：%s", FieldUtils.chn(dir), unifiedSymbol));
			}
			tp.onTrade(trade).forEach(account::onDeal);
		}
	}
	
	public List<PositionField> positionFields() {
		List<PositionField> resultList = new ArrayList<>();
		resultList.addAll(buyPosMap.values().stream().map(tp -> tp.convertToPositionField(account.getAccountDescription().getGatewayId())).toList());
		resultList.addAll(sellPosMap.values().stream().map(tp -> tp.convertToPositionField(account.getAccountDescription().getGatewayId())).toList());
		return resultList;
	}
	
	public List<TradeField> getNonclosedTrade() {
		List<TradeField> resultList = new ArrayList<>();
		resultList.addAll(buyPosMap.values().stream().flatMap(tp -> tp.getUncloseTrades().stream()).toList());
		resultList.addAll(sellPosMap.values().stream().flatMap(tp -> tp.getUncloseTrades().stream()).toList());
		return resultList;
	}
	
	public double totalHoldingProfit() {
		return positionFields().stream().mapToDouble(PositionField::getPositionProfit).sum();
	}
	
	public double totalMargin() {
		return positionFields().stream().mapToDouble(PositionField::getExchangeMargin).sum();
	}

	public int getAvailablePosition(DirectionEnum direction, String unifiedSymbol, boolean reverse) {
		TradePosition tp = getPosition(direction, unifiedSymbol, reverse);
		if(Objects.isNull(tp)) {
			throw new NoSuchElementException(String.format("找不到%s头持仓：%s", FieldUtils.chn(direction), unifiedSymbol));
		}		
		return tp.totalAvailable();
	}
	
	private TradePosition getPosition(DirectionEnum dir, String unifiedSymbol, boolean reverse){
		return getPosMap(dir, reverse).get(unifiedSymbol);
	}
	
	private ConcurrentMap<String, TradePosition> getPosMap(DirectionEnum dir, boolean reverse){
		return switch(dir) {
		case D_Buy -> reverse ? sellPosMap : buyPosMap;
		case D_Sell -> reverse ? buyPosMap : sellPosMap;
		default -> throw new IllegalArgumentException("Unexpected value: " + dir);
		};
	}
}
