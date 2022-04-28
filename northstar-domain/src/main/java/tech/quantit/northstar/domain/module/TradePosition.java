package tech.quantit.northstar.domain.module;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import tech.quantit.northstar.common.constant.ClosingPolicy;
import tech.quantit.northstar.common.utils.FieldUtils;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
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
	
	private ClosingPolicy closingPolicy;

	public TradePosition(List<TradeField> trades, ClosingPolicy closingPolicy) {
		Assert.notEmpty(trades, "不能传入空集合");
		this.closingPolicy = closingPolicy;
		this.trades.addAll(trades);
		this.trades.sort((a, b) -> a.getTradeTimestamp() < b.getTradeTimestamp() ? -1 : 1);
		this.dir = trades.get(0).getDirection();
		this.contract = trades.get(0).getContract();
		for(TradeField trade : trades) {			
			Assert.isTrue(dir == trade.getDirection() && contract.equals(trade.getContract()), "传入的数据不一致");
		}
	}
	
	/**
	 * 更新行情
	 * @param tick
	 */
	public void updateTick(TickField tick) {
		if(contract.getUnifiedSymbol().equals(tick.getUnifiedSymbol())) {
			lastTick = tick;
		}
	}
	
	/**
	 * 订单处理
	 * @param order
	 */
	public void onOrder(OrderField order) {
		if(!contract.equals(order.getContract())	//合约不一致 
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
	public double onTrade(TradeField trade) {
		if(!contract.equals(trade.getContract())) {
			return 0D;
		}
		if(FieldUtils.isClose(trade.getOffsetFlag()) && FieldUtils.isOpposite(dir, trade.getDirection()))	//平仓时，方向要反向
		{
			return closingOpenTrade(trade);
		}
		if(FieldUtils.isOpen(trade.getOffsetFlag()) && trade.getDirection() == dir) //开仓时，方向要同向 
		{
			trades.add(trade);
		}
		return 0D;
	}
	
	private double closingOpenTrade(TradeField trade) {
		double sumProfit = 0;
		int restVol = trade.getVolume();
		int factor = FieldUtils.directionFactor(dir);
		while(restVol > 0 && !trades.isEmpty()) {
			if(closingPolicy == ClosingPolicy.PRIOR_TODAY) {
				TradeField t = trades.pollLast();
				if(t.getVolume() > restVol) {
					trades.offerLast(t.toBuilder().setVolume(t.getVolume() - restVol).build());
					sumProfit += factor * (trade.getPrice() - t.getPrice()) * restVol * contract.getMultiplier();
					restVol = 0;
				} else {
					restVol -= t.getVolume();
					sumProfit += factor * (trade.getPrice() - t.getPrice()) * t.getVolume() * contract.getMultiplier();
				}
			} else {
				TradeField t = trades.pollFirst();
				if(t.getVolume() > restVol) {
					trades.offerFirst(t.toBuilder().setVolume(t.getVolume() - restVol).build());
					sumProfit += factor * (trade.getPrice() - t.getPrice()) * restVol * contract.getMultiplier();
					restVol = 0;
				} else {
					restVol -= t.getVolume();
					sumProfit += factor * (trade.getPrice() - t.getPrice()) * t.getVolume() * contract.getMultiplier();
				}
			}
		}
		return sumProfit;
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
	 * 清理全部委托
	 * 场景：委托单跨交易日时，需要在开盘时处理
	 */
	public void releaseOrders() {
		pendingOrderMap.clear();
	}

}
