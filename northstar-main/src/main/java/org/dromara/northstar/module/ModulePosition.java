package org.dromara.northstar.module;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;

import lombok.Getter;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * 模组持仓
 * 一个实例代表一个合约在一个方向模组中持仓信息
 * @author KevinHuangwl
 *
 */
public class ModulePosition implements TickDataAware, TransactionAware{

	/* 旧成交在队首，新成交在队尾 */
	private final LinkedList<Trade> trades = new LinkedList<>();

	private final Map<String, Order> pendingOrderMap = new HashMap<>();

	private final DirectionEnum direction;

	private Tick lastTick;

	private LocalDate tradingDay;

	private ClosingPolicy closingPolicy;

	/* 开平仓匹配回调 */
	private BiConsumer<Trade, Trade> onDealCallback;

	private String moduleName;

	@Getter
	private final Contract contract;

	public ModulePosition(String moduleName, Contract contract, DirectionEnum direction, ClosingPolicy closingPolicy,
						  BiConsumer<Trade, Trade> onDealCallback) {
		this.moduleName = moduleName;
		this.contract = contract;
		this.direction = direction;
		this.closingPolicy = closingPolicy;
		this.onDealCallback = onDealCallback;
	}

	public ModulePosition(String gatewayId, Contract contract, DirectionEnum direction, ClosingPolicy closingPolicy, BiConsumer<Trade, Trade> onDealCallback,
						  List<Trade> nonclosedTrades) {
		this(gatewayId, contract, direction, closingPolicy, onDealCallback);
		trades.addAll(nonclosedTrades.stream()
				.filter(trade -> trade.direction() == direction)
				.filter(trade -> trade.contract().equals(contract))
				.toList());
	}

	@Override
	public void onTick(Tick tick) {
		if(!contract.equals(tick.contract())) {
			return;
		}
		if(Objects.isNull(tradingDay) || !tradingDay.equals(tick.tradingDay())) {
			pendingOrderMap.clear();
			tradingDay = tick.tradingDay();
		}
		lastTick = tick;
	}

	/**
	 * 订单处理
	 * @param order
	 */
	@Override
	public void onOrder(Order order) {
		if(!contract.equals(order.contract())                //合约不一致
				|| !FieldUtils.isClose(order.offsetFlag())                //不是平仓订单
				|| !FieldUtils.isOpposite(direction, order.direction())        //不是反向订单
				|| order.orderStatus() == OrderStatusEnum.OS_Rejected    //废单
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
	 */
	@Override
	public void onTrade(Trade trade) {
		if(!contract.equals(trade.contract())) {
			return;
		}
		if(FieldUtils.isClose(trade.offsetFlag()) && FieldUtils.isOpposite(direction, trade.direction()))    //平仓时，方向要反向
		{
			closingOpenTrade(trade);
		}
		if(FieldUtils.isOpen(trade.offsetFlag()) && trade.direction() == direction) //开仓时，方向要同向
		{
			trades.add(trade);
		}
	}

	private void closingOpenTrade(Trade closeTrade) {
		int restVol = closeTrade.volume();
		while(restVol > 0 && !trades.isEmpty()) {
			if(closingPolicy == ClosingPolicy.FIRST_IN_LAST_OUT) {
				Trade openTrade = trades.pollLast();
				if(openTrade.volume() > restVol) {
					Trade partOfOpenTrade = openTrade.toBuilder().volume(closeTrade.volume()).build();
					trades.offerLast(openTrade.toBuilder().volume(openTrade.volume() - restVol).build());
					restVol = 0;
					onDealCallback.accept(partOfOpenTrade, closeTrade);
				} else {
					restVol -= openTrade.volume();
					onDealCallback.accept(openTrade, closeTrade.toBuilder().volume(openTrade.volume()).build());
				}
			} else {
				Trade openTrade = trades.pollFirst();
				if(openTrade.volume() > restVol) {
					Trade partOfOpenTrade = openTrade.toBuilder().volume(closeTrade.volume()).build();
					trades.offerFirst(openTrade.toBuilder().volume(openTrade.volume() - restVol).build());
					restVol = 0;
					onDealCallback.accept(partOfOpenTrade, closeTrade);
				} else {
					restVol -= openTrade.volume();
					onDealCallback.accept(openTrade, closeTrade.toBuilder().volume(openTrade.volume()).build());
				}
			}
		}
	}

	/**
	 * 获取未平仓原始成交
	 * @return
	 */
	public List<Trade> getNonclosedTrades(){
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
		LocalDate currentTradingDay = lastTick.tradingDay();
		return trades.stream()
				.filter(t -> currentTradingDay.equals(t.tradingDay()))
				.mapToInt(Trade::volume)
				.reduce(0, Integer::sum);
	}

	/**
	 * 获取非当天持仓
	 * @return
	 */
	public int ydVolume() {
		if(Objects.isNull(lastTick)) return 0;
		LocalDate currentTradingDay = lastTick.tradingDay();
		return trades.stream()
				.filter(t -> !currentTradingDay.equals(t.tradingDay()))
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
		if(totalVol == 0) return 0;
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
		int factor = FieldUtils.directionFactor(direction);
		double priceDiff = factor * (lastTick.lastPrice() - avgOpenPrice());
		return priceDiff * totalVolume() * contract.multiplier();
	}

	/**
	 * 占用保证金
	 * @return
	 */
	public double totalMargin() {
		double ratio = FieldUtils.isBuy(direction) ? contract.longMarginRatio() : contract.shortMarginRatio();
		return totalVolume() * avgOpenPrice() * contract.multiplier() * ratio;
	}

	/**
	 * 持仓信息汇总
	 * @return
	 */
	public Position convertToPosition() {
		int factor = FieldUtils.directionFactor(direction);
		double lastPrice = lastTick == null ? 0 : lastTick.lastPrice();
		double priceDiff = lastTick == null ? 0 : factor * (lastTick.lastPrice() - avgOpenPrice());
		return Position.builder()
				.gatewayId(moduleName)
				.accountId(moduleName)
				.contract(contract)
				.frozen(totalVolume() - totalAvailable())
				.tdFrozen(tdVolume() - tdAvailable())
				.ydFrozen(ydVolume() - ydAvailable())
				.position(totalVolume())
				.tdPosition(tdVolume())
				.ydPosition(ydVolume())
				.exchangeMargin(totalMargin())
				.useMargin(totalMargin())
				.lastPrice(lastPrice)
				.openPrice(avgOpenPrice())
				.openPriceDiff(priceDiff)
				.positionProfit(profit())
				.positionDirection(FieldUtils.isBuy(direction) ? PositionDirectionEnum.PD_Long : PositionDirectionEnum.PD_Short)
				.build();
	}

	public void releaseOrder() {
		pendingOrderMap.clear();
	}
}
