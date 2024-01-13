package org.dromara.northstar.gateway.sim.trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * 交易持仓，代表某个合约一个方向的持仓汇总信息
 * 负责计算加减仓与浮动盈亏
 * @author KevinHuangwl
 *
 */

public class TradePosition {

	/* 旧成交在队首，新成交在队尾 */
	private final LinkedList<Trade> trades = new LinkedList<>();
	
	private final Map<String, Order> pendingOrderMap = new HashMap<>();

	private final DirectionEnum dir;
	
	private final Contract contract;
	
	private Tick lastTick;
	
	public TradePosition(Contract contract, DirectionEnum direction) {
		this.contract = contract;
		this.dir = direction;
	}
	
	/**
	 * 更新行情
	 * @param tick
	 */
	public void onTick(Tick tick) {
		if(contract.equals(tick.contract())) {
			lastTick = tick;
		}
	}
	
	/**
	 * 订单处理
	 * @param order
	 */
	public void onOrder(Order order) {
		if(!contract.equals(order.contract())	//合约不一致 
				|| !FieldUtils.isClose(order.offsetFlag())				//不是平仓订单 
				|| !FieldUtils.isOpposite(dir, order.direction())		//不是反向订单
				|| order.orderStatus() == OrderStatusEnum.OS_Rejected	//废单
			) {
			return;
		}
		if(order.orderStatus() == OrderStatusEnum.OS_AllTraded || order.orderStatus() == OrderStatusEnum.OS_Canceled) {
			pendingOrderMap.remove(order.originOrderId());
		} else {
			pendingOrderMap.put(order.originOrderId(), order);
		}
	}
	
	/**
	 * 加减仓处理
	 * @param trade
	 * @return			返回平仓盈亏
	 */
	public List<Deal> onTrade(Trade trade) {
		if(!contract.equals(trade.contract())) {
			throw new IllegalArgumentException(String.format("不是同一个合约。期望：%s，实际：%s", contract.contractId(), trade.contract().contractId()));
		}
		if(FieldUtils.isClose(trade.offsetFlag()) && FieldUtils.isOpposite(dir, trade.direction()))	//平仓时，方向要反向
		{
			return closingOpenTrade(trade);
		}
		if(FieldUtils.isOpen(trade.offsetFlag()) && trade.direction() == dir) //开仓时，方向要同向 
		{
			trades.add(trade);
		}
		return Collections.emptyList();
	}
	
	private List<Deal> closingOpenTrade(Trade trade) {
		List<Deal> resultList = new ArrayList<>();
		int restVol = trade.volume();
		while(restVol > 0 && !trades.isEmpty()) {
			Trade t = trades.pollFirst();
			if(t.volume() > restVol) {
				Trade openTrade = t.toBuilder().volume(restVol).build();
				Trade restTrade = t.toBuilder().volume(t.volume() - restVol).build();
				trades.offerFirst(restTrade);
				resultList.add(Deal.builder().openTrade(openTrade).closeTrade(trade).build());
				restVol = 0;
			} else {
				restVol -= t.volume();
				Trade openTrade = t;
				Trade closeTrade = trade.toBuilder().volume(t.volume()).build();
				resultList.add(Deal.builder().openTrade(openTrade).closeTrade(closeTrade).build());
			}
		}
		return resultList;
	}
	
	/**
	 * 获取未平仓原始成交
	 * @return
	 */
	public List<Trade> getUncloseTrades(){
		return trades;
	}
	
	/**
	 * 获取合计持仓
	 * @return
	 */
	public int totalVolume() {
		return trades.stream()
				.mapToInt(Trade::volume)
				.reduce(0, Integer::sum);
	}
	
	/**
	 * 获取当前持仓
	 * @return
	 */
	public int tdVolume() {
		if(Objects.isNull(lastTick)) return 0;
		return trades.stream()
				.filter(t -> Objects.equals(t.tradingDay(), lastTick.tradingDay()))
				.mapToInt(Trade::volume)
				.reduce(0, Integer::sum);
	}
	
	/**
	 * 获取非当天持仓
	 * @return
	 */
	public int ydVolume() {
		if(Objects.isNull(lastTick)) return 0;
		return trades.stream()
				.filter(t -> !Objects.equals(t.tradingDay(), lastTick.tradingDay()))
				.mapToInt(Trade::volume)
				.reduce(0, Integer::sum);
	}
	
	/**
	 * 获取总可用手数
	 * @return
	 */
	public int totalAvailable() {
		int frozen = pendingOrderMap.values().stream()
				.mapToInt(Order::totalVolume)
				.reduce(0, Integer::sum);
		return totalVolume() - frozen;
	}
	
	/**
	 * 获取当天可用手数
	 * @return
	 */
	public int tdAvailable() {
		int tdFrozen = pendingOrderMap.values().stream()
				.filter(order -> order.offsetFlag() == OffsetFlagEnum.OF_CloseToday)
				.mapToInt(Order::totalVolume)
				.reduce(0, Integer::sum);
		return tdVolume() - tdFrozen;
	}
	
	/**
	 * 获取非当天可用手数
	 * @return
	 */
	public int ydAvailable() {
		int ydFrozen = pendingOrderMap.values().stream()
				.filter(order -> order.offsetFlag() != OffsetFlagEnum.OF_CloseToday)
				.mapToInt(Order::totalVolume)
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
				.mapToDouble(t -> t.volume() * t.price())
				.reduce(0D, Double::sum) / totalVol;
	}
	
	/**
	 * 持仓盈亏
	 * @return
	 */
	public double profit() {
		if(Objects.isNull(lastTick)) return 0;
		int factor = FieldUtils.directionFactor(dir);
		double priceDiff = factor * (lastTick.lastPrice() - avgOpenPrice());
		return priceDiff * totalVolume() * contract.multiplier();
	}
	
	/**
	 * 占用保证金
	 * @return
	 */
	public double totalMargin() {
		double ratio = FieldUtils.isBuy(dir) ? contract.longMarginRatio() : contract.shortMarginRatio();
		return totalVolume() * avgOpenPrice() * contract.multiplier() * ratio;
	}
	
	/**
	 * 持仓信息汇总
	 * @return
	 */
	public Position convertToPosition(String gatewayId) {
		int factor = FieldUtils.directionFactor(dir);
		double priceDiff = lastTick == null ? 0 : factor * (lastTick.lastPrice() - avgOpenPrice());
		PositionDirectionEnum posDir = FieldUtils.isBuy(dir) ? PositionDirectionEnum.PD_Long : PositionDirectionEnum.PD_Short;
		return Position.builder()
				.gatewayId(gatewayId)
				.positionId(contract.unifiedSymbol() + "@" + posDir)
				.contract(contract)
				.frozen(totalVolume() - totalAvailable())
				.tdFrozen(tdVolume() - tdAvailable())
				.ydFrozen(ydVolume() - ydAvailable())
				.position(totalVolume())
				.tdPosition(tdVolume())
				.ydPosition(ydVolume())
				.exchangeMargin(totalMargin())
				.useMargin(totalMargin())
				.openPrice(avgOpenPrice())
				.openPriceDiff(priceDiff)
				.positionProfit(profit())
				.positionDirection(posDir)
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
