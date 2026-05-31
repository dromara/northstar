package org.dromara.northstar.gateway.sim.trade;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.model.DealRecord;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.data.IAccountRepository;

import lombok.Getter;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * 账户持仓
 * 一个实例代表账户中一个合约在一个方向的持仓信息
 * @author KevinHuangwl
 *
 */
public class AccountPosition implements TickDataAware, TransactionAware{

	/* 旧成交在队首，新成交在队尾 */
	private final LinkedList<Trade> trades = new LinkedList<>();

	/* 用于计算持仓占用 */
	private final Map<String, Order> pendingOrderMap = new HashMap<>();

	private final IAccountRepository repo;

	private Tick lastTick;
	
	@Getter
	private double accRealizedProfit;
	
	@Getter
	private double accCommission;
	
	@Getter
	private final String accountName;
	
	@Getter
	private final PositionDirectionEnum direction;

	@Getter
	private final Contract contract;
	
	public AccountPosition(String accountName, Contract contract, PositionDirectionEnum direction, IAccountRepository repo) {
		this.contract = contract;
		this.direction = direction;
		this.repo = repo;
		this.accountName = accountName;
	}
	
	public AccountPosition(String accountName, Contract contract, PositionDirectionEnum direction, IAccountRepository repo, List<Trade> trades) {
		this.contract = contract;
		this.direction = direction;
		this.repo = repo;
		this.accountName = accountName;
		if(!trades.isEmpty()) {			
			this.trades.addAll(trades);
		}
	}

	private void saveDealRecord(Trade openTrade, Trade closeTrade) {
		int factor = FieldUtils.directionFactor(openTrade.direction());
		double profit = factor * (closeTrade.price() - openTrade.price()) * closeTrade.volume() * contract.multiplier();
		ContractDefinition cd = contract.contractDefinition();
		double commission = cd.commissionFee() != null ? cd.commissionFee() : cd.commissionRate() * closeTrade.price() * contract.multiplier();
		double dealCommission = commission * 2 * closeTrade.volume(); // 乘2代表手续费双向计算
		accRealizedProfit += profit;
		accCommission += dealCommission;
		repo.saveDealRecord(DealRecord.builder()
				.accountName(accountName)
				.dealProfit(profit)
				.transactionFee(dealCommission)
				.openTrade(openTrade.toTradeField().toByteArray())
				.closeTrade(closeTrade.toTradeField().toByteArray())
				.build());
	}

	@Override
	public void onTick(Tick tick) {
		if(!contract.equals(tick.contract())) {
			return;
		}
		lastTick = tick;
	}

	/**
	 * 订单处理
	 * @param order
	 */
	@Override
	public synchronized void onOrder(Order order) {
		if(!contract.equals(order.contract())                //合约不一致
				|| !FieldUtils.isClose(order.offsetFlag())                //不是平仓订单
				|| FieldUtils.closePositionDirOf(order.direction()) != direction        //不是本方向的平仓订单
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
	public synchronized void onTrade(Trade trade) {
		if(!contract.equals(trade.contract()) 
				|| FieldUtils.isOpen(trade.offsetFlag()) && FieldUtils.openPositionDirOf(trade.direction()) != direction
				|| FieldUtils.isClose(trade.offsetFlag()) && FieldUtils.closePositionDirOf(trade.direction()) != direction) {
			return;
		}
		if(FieldUtils.isClose(trade.offsetFlag()))    //平仓时，方向要反向
		{
			closingOpenTrade(trade);
		}
		if(FieldUtils.isOpen(trade.offsetFlag())) //开仓时，方向要同向
		{
			trades.add(trade);
		}
	}

	private void closingOpenTrade(Trade closeTrade) {
		double restVol = closeTrade.volume();
		while(restVol > 0 && !trades.isEmpty()) {
			Trade openTrade = trades.pollFirst();
			if(openTrade.volume() > restVol) {
				Trade partOfOpenTrade = openTrade.toBuilder().tradeId(UUID.randomUUID().toString()).volume(restVol).build();
				trades.offerFirst(openTrade.toBuilder().volume(openTrade.volume() - restVol).build());
				saveDealRecord(partOfOpenTrade, closeTrade.toBuilder().tradeId(UUID.randomUUID().toString()).volume(restVol).build());
				restVol = 0;
			} else {
				restVol -= openTrade.volume();
				saveDealRecord(openTrade, closeTrade.toBuilder().tradeId(UUID.randomUUID().toString()).volume(openTrade.volume()).build());
			}
		}
	}

	/**
	 * 获取未平仓原始成交
	 * @return
	 */
	public List<Trade> getOpeningTrades(){
		return trades;
	}

	/**
	 * 获取合计持仓
	 * @return
	 */
	public double position() {
		return trades.stream()
				.mapToDouble(Trade::volume)
				.reduce(0, Double::sum);
	}

	/**
	 * 获取总可用交易量
	 * @return
	 */
	public double frozen() {
		return pendingOrderMap.values().stream()
				.mapToDouble(Order::totalVolume)
				.reduce(0, Double::sum);
	}

	/**
	 * 加权平均成本价
	 * @return
	 */
	public double avgOpenPrice() {
		double totalVol = position();
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
		return priceDiff * position() * contract.multiplier();
	}

	/**
	 * 占用保证金
	 * @return
	 */
	public double margin() {
		double ratio = contract.contractDefinition().marginRatio();
		return position() * avgOpenPrice() * contract.multiplier() * ratio;
	}

	/**
	 * 持仓信息汇总
	 * @return
	 */
	public Position convertToPosition() {
		int factor = FieldUtils.directionFactor(direction);
		double priceDiff = lastTick == null ? 0 : factor * (lastTick.lastPrice() - avgOpenPrice());
		return Position.builder()
				.gatewayId(accountName)
				.positionId(String.format("%s@%s", contract.contractId(), direction))
				.contract(contract)
				.position(position())
				.frozen(frozen())
				.exchangeMargin(margin())
				.useMargin(margin())
				.openPrice(avgOpenPrice())
				.openPriceDiff(priceDiff)
				.lastTick(lastTick)
				.positionProfit(profit())
				.positionDirection(direction)
				.build();
	}

}
