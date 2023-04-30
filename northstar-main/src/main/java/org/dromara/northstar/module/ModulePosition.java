package org.dromara.northstar.module;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.apache.commons.codec.binary.StringUtils;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.utils.ContractUtils;
import org.dromara.northstar.common.utils.FieldUtils;

import lombok.Getter;
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
 * 模组持仓
 * 一个实例代表一个合约在一个方向模组中持仓信息
 * @author KevinHuangwl
 *
 */
public class ModulePosition implements TickDataAware, TransactionAware{

	/* 旧成交在队首，新成交在队尾 */
	private final LinkedList<TradeField> trades = new LinkedList<>();
	
	private final Map<String, OrderField> pendingOrderMap = new HashMap<>();

	private final DirectionEnum direction;
	
	private TickField lastTick;
	
	private String tradingDay;
	
	private ClosingPolicy closingPolicy;
	
	/* 开平仓匹配回调 */
	private BiConsumer<TradeField, TradeField> onDealCallback;
	
	private String gatewayId;
	
	@Getter
	private final ContractField contract;

	public ModulePosition(String gatewayId, ContractField contract, DirectionEnum direction, ClosingPolicy closingPolicy, 
			BiConsumer<TradeField, TradeField> onDealCallback) {
		this.gatewayId = gatewayId;
		this.contract = contract;
		this.direction = direction;
		this.closingPolicy = closingPolicy;
		this.onDealCallback = onDealCallback;
	}
	
	public ModulePosition(String gatewayId, ContractField contract, DirectionEnum direction, ClosingPolicy closingPolicy, BiConsumer<TradeField, TradeField> onDealCallback,
			List<TradeField> nonclosedTrades) {
		this(gatewayId, contract, direction, closingPolicy, onDealCallback);
		trades.addAll(nonclosedTrades.stream()
				.filter(trade -> trade.getDirection() == direction)
				.filter(trade -> StringUtils.equals(trade.getContract().getContractId(), contract.getContractId()))
				.toList());
	}
	
	@Override
	public void onTick(TickField tick) {
		if(!StringUtils.equals(contract.getUnifiedSymbol(), tick.getUnifiedSymbol())) {
			return;
		}
		if(!StringUtils.equals(tradingDay, tick.getTradingDay())) {
			pendingOrderMap.clear();
			tradingDay = tick.getTradingDay();
		}
		lastTick = tick;
	}
	
	/**
	 * 订单处理
	 * @param order
	 */
	@Override
	public void onOrder(OrderField order) {
		if(!ContractUtils.isSame(contract, order.getContract())				//合约不一致 
				|| !FieldUtils.isClose(order.getOffsetFlag())				//不是平仓订单 
				|| !FieldUtils.isOpposite(direction, order.getDirection())		//不是反向订单
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
	 */
	@Override
	public void onTrade(TradeField trade) {
		if(!ContractUtils.isSame(contract, trade.getContract())) {
			return;
		}
		if(FieldUtils.isClose(trade.getOffsetFlag()) && FieldUtils.isOpposite(direction, trade.getDirection()))	//平仓时，方向要反向
		{
			closingOpenTrade(trade);
		}
		if(FieldUtils.isOpen(trade.getOffsetFlag()) && trade.getDirection() == direction) //开仓时，方向要同向 
		{
			trades.add(trade);
		}
	}
	
	private void closingOpenTrade(TradeField closeTrade) {
		int restVol = closeTrade.getVolume();
		while(restVol > 0 && !trades.isEmpty()) {
			if(closingPolicy == ClosingPolicy.FIRST_IN_LAST_OUT) {
				TradeField openTrade = trades.pollLast();
				if(openTrade.getVolume() > restVol) {
					TradeField partOfOpenTrade = TradeField.newBuilder(openTrade).setVolume(closeTrade.getVolume()).build();
					trades.offerLast(openTrade.toBuilder().setVolume(openTrade.getVolume() - restVol).build());
					restVol = 0;
					onDealCallback.accept(partOfOpenTrade, closeTrade);
				} else {
					restVol -= openTrade.getVolume();
					onDealCallback.accept(openTrade, TradeField.newBuilder(closeTrade).setVolume(openTrade.getVolume()).build());
				}
			} else {
				TradeField openTrade = trades.pollFirst();
				if(openTrade.getVolume() > restVol) {
					TradeField partOfOpenTrade = TradeField.newBuilder(openTrade).setVolume(closeTrade.getVolume()).build();
					trades.offerFirst(openTrade.toBuilder().setVolume(openTrade.getVolume() - restVol).build());
					restVol = 0;
					onDealCallback.accept(partOfOpenTrade, closeTrade);
				} else {
					restVol -= openTrade.getVolume();
					onDealCallback.accept(openTrade, TradeField.newBuilder(closeTrade).setVolume(openTrade.getVolume()).build());
				}
			}
		}
	}
	
	/**
	 * 获取未平仓原始成交
	 * @return
	 */
	public List<TradeField> getNonclosedTrades(){
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
		int factor = FieldUtils.directionFactor(direction);
		double priceDiff = factor * (lastTick.getLastPrice() - avgOpenPrice());
		return priceDiff * totalVolume() * contract.getMultiplier();
	}
	
	/**
	 * 占用保证金
	 * @return
	 */
	public double totalMargin() {
		double ratio = FieldUtils.isBuy(direction) ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		return totalVolume() * avgOpenPrice() * contract.getMultiplier() * ratio;
	}
	
	/**
	 * 持仓信息汇总
	 * @return
	 */
	public PositionField convertToPositionField() {
		int factor = FieldUtils.directionFactor(direction);
		double lastPrice = lastTick == null ? 0 : lastTick.getLastPrice();
		double priceDiff = lastTick == null ? 0 : factor * (lastTick.getLastPrice() - avgOpenPrice());
		return PositionField.newBuilder()
				.setGatewayId(gatewayId)
				.setAccountId(gatewayId)
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
				.setPositionDirection(FieldUtils.isBuy(direction) ? PositionDirectionEnum.PD_Long : PositionDirectionEnum.PD_Short)
				.build();
	}

	public void releaseOrder() {
		pendingOrderMap.clear();
	}
}
