package org.dromara.northstar.gateway.sim.trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.utils.FieldUtils;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 交易持仓，代表某个合约一个方向的持仓汇总信息
 * 负责计算加减仓与浮动盈亏
 * @author KevinHuangwl
 *
 */

public class TradePosition {

	/* 旧成交在队首，新成交在队尾 */
	private final LinkedList<TradeField> trades = new LinkedList<>();
	
	private final Map<String, OrderField> pendingOrderMap = new HashMap<>();

	private final DirectionEnum dir;
	
	private final ContractField contract;
	
	private TickField lastTick;
	
	public TradePosition(ContractField contract, DirectionEnum direction) {
		this.contract = contract;
		this.dir = direction;
	}
	
	/**
	 * 更新行情
	 * @param tick
	 */
	public void onTick(TickField tick) {
		if(contract.getUnifiedSymbol().equals(tick.getUnifiedSymbol())) {
			lastTick = tick;
		}
	}
	
	/**
	 * 订单处理
	 * @param order
	 */
	public void onOrder(OrderField order) {
		if(!contract.getContractId().equals(order.getContract().getContractId())	//合约不一致 
				|| !FieldUtils.isClose(order.getOffsetFlag())				//不是平仓订单 
				|| !FieldUtils.isOpposite(dir, order.getDirection())		//不是反向订单
				|| order.getOrderStatus() == OrderStatusEnum.OS_Rejected	//废单
			) {
			return;
		}
		if(order.getOrderStatus() == OrderStatusEnum.OS_AllTraded || order.getOrderStatus() == OrderStatusEnum.OS_Canceled) {
			pendingOrderMap.remove(order.getOriginOrderId());
		} else {
			pendingOrderMap.put(order.getOriginOrderId(), order);
		}
	}
	
	/**
	 * 加减仓处理
	 * @param trade
	 * @return			返回平仓盈亏
	 */
	public List<Deal> onTrade(TradeField trade) {
		if(!contract.getContractId().equals(trade.getContract().getContractId())) {
			throw new IllegalArgumentException(String.format("不是同一个合约。期望：%s，实际：%s", contract.getContractId(), trade.getContract().getContractId()));
		}
		if(FieldUtils.isClose(trade.getOffsetFlag()) && FieldUtils.isOpposite(dir, trade.getDirection()))	//平仓时，方向要反向
		{
			return closingOpenTrade(trade);
		}
		if(FieldUtils.isOpen(trade.getOffsetFlag()) && trade.getDirection() == dir) //开仓时，方向要同向 
		{
			trades.add(trade);
		}
		return Collections.emptyList();
	}
	
	private List<Deal> closingOpenTrade(TradeField trade) {
		List<Deal> resultList = new ArrayList<>();
		int restVol = trade.getVolume();
		while(restVol > 0 && !trades.isEmpty()) {
			TradeField t = trades.pollFirst();
			if(t.getVolume() > restVol) {
				TradeField openTrade = t.toBuilder().setVolume(restVol).build();
				TradeField restTrade = t.toBuilder().setVolume(t.getVolume() - restVol).build();
				trades.offerFirst(restTrade);
				resultList.add(Deal.builder().openTrade(openTrade).closeTrade(trade).build());
				restVol = 0;
			} else {
				restVol -= t.getVolume();
				TradeField openTrade = t;
				TradeField closeTrade = trade.toBuilder().setVolume(t.getVolume()).build();
				resultList.add(Deal.builder().openTrade(openTrade).closeTrade(closeTrade).build());
			}
		}
		return resultList;
	}
	
	/**
	 * 获取未平仓原始成交
	 * @return
	 */
	public List<TradeField> getUncloseTrades(){
		return trades;
	}
	
	/**
	 * 获取合计持仓
	 * @return
	 */
	public int totalVolume() {
		return trades.stream()
				.map(TradeField::getVolume)
				.reduce(0, Integer::sum);
	}
	
	/**
	 * 获取当前持仓
	 * @return
	 */
	public int tdVolume() {
		if(Objects.isNull(lastTick)) return 0;
		String currentTradingDay = lastTick.getTradingDay();
		return trades.stream()
				.filter(t -> StringUtils.equals(currentTradingDay, t.getTradingDay()))
				.map(TradeField::getVolume)
				.reduce(0, Integer::sum);
	}
	
	/**
	 * 获取非当天持仓
	 * @return
	 */
	public int ydVolume() {
		if(Objects.isNull(lastTick)) return 0;
		String currentTradingDay = lastTick.getTradingDay();
		return trades.stream()
				.filter(t -> !StringUtils.equals(currentTradingDay, t.getTradingDay()))
				.map(TradeField::getVolume)
				.reduce(0, Integer::sum);
	}
	
	/**
	 * 获取总可用手数
	 * @return
	 */
	public int totalAvailable() {
		int frozen = pendingOrderMap.values().stream()
				.map(OrderField::getTotalVolume)
				.reduce(0, Integer::sum);
		return totalVolume() - frozen;
	}
	
	/**
	 * 获取当天可用手数
	 * @return
	 */
	public int tdAvailable() {
		int tdFrozen = pendingOrderMap.values().stream()
				.filter(order -> order.getOffsetFlag() == OffsetFlagEnum.OF_CloseToday)
				.map(OrderField::getTotalVolume)
				.reduce(0, Integer::sum);
		return tdVolume() - tdFrozen;
	}
	
	/**
	 * 获取非当天可用手数
	 * @return
	 */
	public int ydAvailable() {
		int ydFrozen = pendingOrderMap.values().stream()
				.filter(order -> order.getOffsetFlag() != OffsetFlagEnum.OF_CloseToday)
				.map(OrderField::getTotalVolume)
				.reduce(0, Integer::sum);
		return ydVolume() - ydFrozen;
	}
	
	/**
	 * 加权平均成本价
	 * @return
	 */
	public double avgOpenPrice() {
		int totalVol = totalVolume();
		if(totalVol == 0) 	return 0;
		return trades.stream()
				.map(t -> t.getVolume() * t.getPrice())
				.reduce(0D, Double::sum) / totalVol;
	}
	
	/**
	 * 持仓盈亏
	 * @return
	 */
	public double profit() {
		if(Objects.isNull(lastTick)) return 0;
		int factor = FieldUtils.directionFactor(dir);
		double priceDiff = factor * (lastTick.getLastPrice() - avgOpenPrice());
		return priceDiff * totalVolume() * contract.getMultiplier();
	}
	
	/**
	 * 占用保证金
	 * @return
	 */
	public double totalMargin() {
		double ratio = FieldUtils.isBuy(dir) ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		return totalVolume() * avgOpenPrice() * contract.getMultiplier() * ratio;
	}
	
	/**
	 * 持仓信息汇总
	 * @return
	 */
	public PositionField convertToPositionField(String gatewayId) {
		int factor = FieldUtils.directionFactor(dir);
		double lastPrice = lastTick == null ? 0 : lastTick.getLastPrice();
		double priceDiff = lastTick == null ? 0 : factor * (lastTick.getLastPrice() - avgOpenPrice());
		PositionDirectionEnum posDir = FieldUtils.isBuy(dir) ? PositionDirectionEnum.PD_Long : PositionDirectionEnum.PD_Short;
		return PositionField.newBuilder()
				.setGatewayId(gatewayId)
				.setPositionId(contract.getUnifiedSymbol() + "@" + posDir)
				.setContract(contract)
				.setFrozen(totalVolume() - totalAvailable())
				.setTdFrozen(tdVolume() - tdAvailable())
				.setYdFrozen(ydVolume() - ydAvailable())
				.setPosition(totalVolume())
				.setTdPosition(tdVolume())
				.setYdPosition(ydVolume())
				.setExchangeMargin(totalMargin())
				.setUseMargin(totalMargin())
				.setLastPrice(lastPrice)
				.setOpenPrice(avgOpenPrice())
				.setOpenPriceDiff(priceDiff)
				.setPrice(avgOpenPrice())
				.setPriceDiff(priceDiff)
				.setPositionProfit(profit())
				.setOpenPositionProfit(profit())
				.setPositionDirection(posDir)
				.build();
	}
	
	/**
	 * 清理全部委托
	 * 场景：委托单跨交易日时，需要在开盘时处理
	 */
	public void releaseOrders() {
		pendingOrderMap.clear();
	}

}
