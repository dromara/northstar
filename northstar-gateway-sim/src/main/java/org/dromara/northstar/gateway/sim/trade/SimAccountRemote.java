package org.dromara.northstar.gateway.sim.trade;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.model.DealRecord;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.data.IAccountRepository;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.strategy.IAccount;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * 模拟交易所服务器的账户
 */

@Slf4j
public class SimAccountRemote implements IAccount, TickDataAware {
	
	private SimTradeGateway gateway;
	
	private Account account;
	
	private IAccountRepository repo;
	
	private MarketCenter mktCenter;
	
	private Table<Contract, PositionDirectionEnum, AccountPosition> positionHolder = HashBasedTable.create(); 
	
	private ConcurrentMap<String, Order> pendingOpen = new ConcurrentHashMap<>();
	private ConcurrentMap<String, Order> pendingClose = new ConcurrentHashMap<>();
	private ConcurrentMap<String, Order> pendingOrders = new ConcurrentHashMap<>();
	
	public SimAccountRemote(MarketCenter mktCenter, IAccountRepository repo) {
		this.repo = repo;
		this.mktCenter = mktCenter;
	}
	
	public void setGateway(SimTradeGateway gateway) {
		this.gateway = gateway;
		String gatewayId = gateway.getDescription().getGatewayId();
		this.account = Optional.ofNullable(repo.findAccountsAsAggregateAccount(gatewayId)).orElse(Account.builder().build());
		// 初始化账户持仓
		Table<Contract, PositionDirectionEnum, List<Trade>> positionTrades = repo.findHoldingTrades(gatewayId);
		positionTrades.cellSet().forEach(entry -> {
			AccountPosition ap = new AccountPosition(getId(), entry.getRowKey(), entry.getColumnKey(), repo, entry.getValue());
			positionHolder.put(entry.getRowKey(), entry.getColumnKey(), ap);
		});
		// 初始化账户平仓盈亏与手续费
		List<DealRecord> dealRecords = repo.findDealRecords(gatewayId);
		double accRealizedProfit = 0;
		double accCommission = 0;
		for(DealRecord dr : dealRecords) {
			accRealizedProfit += dr.getDealProfit();
			accCommission += dr.getTransactionFee();
		}
		account = account.toBuilder()
				.realizedProfit(accRealizedProfit)
				.commission(accCommission)
				.build();
	}
	
	public void pollData() {
		gateway.onAccount(Account.builder()
				.gatewayId(getId())
				.balance(accountBalance())
				.available(availableBalance())
				.build());
		
		positionHolder.values().stream().forEach(ap -> gateway.onPosition(ap.convertToPosition()));
	}

	@Override
	public void onOrder(Order order) {
		if(OrderUtils.isPendingOrder(order)) {
			pendingOrders.put(order.originOrderId(), order);
			if(FieldUtils.isOpen(order.offsetFlag())) {
				pendingOpen.put(order.originOrderId(), order);
			} else {
				pendingClose.put(order.originOrderId(), order);
			}
		}
		if(OrderUtils.isDoneOrder(order)) {
			pendingOrders.remove(order.originOrderId());
			pendingClose.remove(order.originOrderId());
			pendingOpen.remove(order.originOrderId());
		}
		if(FieldUtils.isClose(order.offsetFlag())) {			
			positionHolder.get(order.contract(), FieldUtils.closePositionDirOf(order.direction())).onOrder(order);
		}
		gateway.onOrder(order);
	}

	@Override
	public void onTrade(Trade trade) {
		log.info("[{}] 模拟成交：{}，{}，{}，数量：{}，成交价：{}，订单ID：{}", trade.gatewayId(), trade.contract().name(), trade.direction(),
				trade.offsetFlag(), trade.volume(), trade.price(), trade.originOrderId());
		PositionDirectionEnum posDir = FieldUtils.isOpen(trade.offsetFlag()) ? FieldUtils.openPositionDirOf(trade.direction()) : FieldUtils.closePositionDirOf(trade.direction());
		if(!positionHolder.contains(trade.contract(), posDir)) {
			positionHolder.put(trade.contract(), posDir, new AccountPosition(getId(), trade.contract(), posDir, repo));
		}
		positionHolder.get(trade.contract(), posDir).onTrade(trade);
		repo.saveHoldingTrades(getId(),trade.contract(), posDir, positionHolder.get(trade.contract(), posDir).getOpeningTrades());
		gateway.onTrade(trade);
	}

	@Override
	public void submitOrder(SubmitOrderReq orderReq) throws TradeException {
		try {
			validate(orderReq);
			String orderId = UUID.randomUUID().toString();
			boolean immediateTrade = orderReq.price() == 0;
			if(immediateTrade) {
				Optional<Tick> lastTick = mktCenter.lastTick(orderReq.contract());
				Tick tick = lastTick.get();
				
				onOrder(Order.builder()
						.gatewayId(getId())
						.originOrderId(orderReq.originOrderId())
						.orderId(orderId)
						.contract(orderReq.contract())
						.direction(orderReq.direction())
						.offsetFlag(orderReq.offsetFlag())
						.orderPriceType(OrderPriceTypeEnum.OPT_AnyPrice)
						.totalVolume(orderReq.volume())
						.tradedVolume(orderReq.volume())
						.price(tick.lastPrice())
						.orderStatus(OrderStatusEnum.OS_AllTraded)
						.statusMsg("全部成交")
						.build());
				onTrade(Trade.builder()
						.gatewayId(getId())
						.originOrderId(orderReq.originOrderId())
						.orderId(orderId)
						.tradeId(UUID.randomUUID().toString())
						.contract(tick.contract())
						.tradingDay(tick.tradingDay())
						.tradeDate(tick.actionDay())
						.tradeTime(tick.actionTime())
						.tradeTimestamp(tick.actionTimestamp())
						.direction(orderReq.direction())
						.offsetFlag(orderReq.offsetFlag())
						.volume(orderReq.volume())
						.price(tick.lastPrice())
						.build());
			} else {
				onOrder(Order.builder()
						.gatewayId(getId())
						.originOrderId(orderReq.originOrderId())
						.orderId(orderId)
						.contract(orderReq.contract())
						.direction(orderReq.direction())
						.offsetFlag(orderReq.offsetFlag())
						.orderPriceType(OrderPriceTypeEnum.OPT_LimitPrice)
						.totalVolume(orderReq.volume())
						.price(orderReq.price())
						.orderStatus(OrderStatusEnum.OS_Touched)
						.statusMsg("已报单")
						.build());
				log.info("[{}] 成功下单：{}，{}，{}，数量：{}，委托价：{}，订单ID：{}", orderReq.gatewayId(),
					orderReq.contract().name(), orderReq.direction(), orderReq.offsetFlag(),
					orderReq.volume(), orderReq.price(), orderReq.originOrderId());
			}
		} catch(IllegalStateException e) {
			onOrder(Order.builder()
					.gatewayId(getId())
					.originOrderId(orderReq.originOrderId())
					.contract(orderReq.contract())
					.direction(orderReq.direction())
					.offsetFlag(orderReq.offsetFlag())
					.orderPriceType(orderReq.price() == 0 ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
					.totalVolume(orderReq.volume())
					.price(orderReq.price())
					.orderStatus(OrderStatusEnum.OS_Rejected)
					.statusMsg(e.getMessage())
					.build());
		}
	}
	
	private void validate(SubmitOrderReq orderReq) {
		if(orderReq.offsetFlag() == OffsetFlagEnum.OF_Unknown) {
			throw new IllegalStateException("非法委托操作");
		}
		Optional<Tick> lastTick = mktCenter.lastTick(orderReq.contract());
		if(lastTick.isEmpty()) {
			throw new IllegalStateException("没有行情数据，无法下单");
		}
		Tick tick = lastTick.get();
		
		if(orderReq.offsetFlag() == OffsetFlagEnum.OF_Open) {
			double cost = cost(orderReq, tick);
			double available = availableBalance();
			if(cost > available) {
				String msg = String.format("[%s] 可用资金不足，无法开仓。可用：%.2f，实际开仓：%.2f", orderReq.gatewayId(), available, cost);
				throw new IllegalStateException(msg);
			}
		} else {
			PositionDirectionEnum posDir = FieldUtils.closePositionDirOf(orderReq.direction());
			Optional<Position> position = getPosition(posDir, orderReq.contract());
			if(position.isEmpty()) {
				String msg = String.format("[%s] 没有 %s 的持仓", orderReq.gatewayId(), orderReq.contract().name());
				throw new IllegalStateException(msg);
			}
			if(orderReq.volume() > (position.get().position() - position.get().frozen())) {
				String msg = String.format("[%s] 可用持仓不足，无法平仓。可用：%.2f，实际平仓：%.2f", orderReq.gatewayId(), (position.get().position() - position.get().frozen()), orderReq.volume());
				throw new IllegalStateException(msg);
			}
		}
	}
	
	/**
	 * 订单占用资金
	 * @return
	 */
	public double cost(SubmitOrderReq submitOrderReq, Tick tick) {
		Contract contract = submitOrderReq.contract();
		double vol = submitOrderReq.volume();
		double multiplier = contract.multiplier();
		double price = tick.lastPrice();
		double marginRatio = contract.contractDefinition().marginRatio();
		return price * vol * multiplier * marginRatio * Math.pow(10, -contract.quantityPrecision());
	}

	@Override
	public boolean cancelOrder(CancelOrderReq cancelReq) {
		if(!pendingOrders.containsKey(cancelReq.originOrderId())) {
			log.info("委托单不存在：{}", cancelReq.originOrderId());
			return false;
		}
		Order pendingOrder = pendingOrders.remove(cancelReq.originOrderId());
		Order cancelledOrder = pendingOrder.toBuilder()
				.updateDate(LocalDate.now())
				.updateTime(LocalTime.now())
				.orderStatus(OrderStatusEnum.OS_Canceled)
				.statusMsg("已撤单")
				.build();
		onOrder(cancelledOrder);
		log.info("[{}] 成功撤单：{}", getId(), cancelReq.originOrderId());
		return true;
	}
	
	public double moneyIO(int money) {
		if(money > 0) {
			repo.saveAccount(Account.builder().gatewayId(getId()).deposit(money).build());
			account = account.toBuilder().deposit(money + account.deposit()).build();
		}
		if(money < 0) {
			repo.saveAccount(Account.builder().gatewayId(getId()).withdraw(Math.abs(money)).build());
			account = account.toBuilder().withdraw(Math.abs(money) + account.withdraw()).build();
		}
		return accountBalance();
	}

	@Override
	public double accountBalance() {
		double unrealizedProfit = positionHolder.values().stream().mapToDouble(ap -> ap.profit()).sum();
		double realizedProfit = positionHolder.values().stream().mapToDouble(ap -> ap.getAccRealizedProfit()).sum() + account.realizedProfit();
		double commission =  positionHolder.values().stream().mapToDouble(ap -> ap.getAccCommission()).sum() + account.commission();
		return realizedProfit + unrealizedProfit - commission + account.deposit() - account.withdraw();
	}

	@Override
	public double availableBalance() {
		return accountBalance() - useMargin();
	}
	
	/**
	 * 保证金占用
	 * @return
	 */
	private double useMargin() {
		double positionMargin = positionHolder.values().stream()
				.mapToDouble(ap -> ap.margin())
				.sum();
		double pendingOpenOrderMargin = pendingOpen.values().stream()
				.mapToDouble(order -> {
					double ratio = order.contract().contractDefinition().marginRatio();
					return order.totalVolume() * order.price() * order.contract().multiplier() * ratio;
				})
				.sum();
		return pendingOpenOrderMargin + positionMargin;
	}

	@Override
	public String getId() {
		return gateway.getDescription().getGatewayId();
	}

	@Override
	public Optional<Position> getPosition(PositionDirectionEnum posDirection, Contract contract) {
		AccountPosition accPos = positionHolder.get(contract, posDirection);
		if(accPos == null) {
			return Optional.empty();
		}
		return Optional.of(accPos.convertToPosition());
	}

	@Override
	public List<Order> getPendingOrders() {
		return pendingOrders.values().stream().toList();
	}

	@Override
	public TradeGateway getTradeGateway() {
		return gateway;
	}

	@Override
	public void onTick(Tick tick) {
		Set<String> finishedSet = new HashSet<>();
		positionHolder.values().forEach(ap -> ap.onTick(tick));
		pendingOrders.values().stream().filter(req -> req.contract().equals(tick.contract())).forEach(req -> {
			boolean matched = req.price() == 0 
					|| (req.direction() == DirectionEnum.D_Buy && tick.lastPrice() < req.price()) 
					|| (req.direction() == DirectionEnum.D_Sell && tick.lastPrice() > req.price());
			if(matched) {
				String orderId = UUID.randomUUID().toString();
				onOrder(Order.builder()
						.gatewayId(getId())
						.originOrderId(req.originOrderId())
						.orderId(orderId)
						.contract(tick.contract())
						.tradingDay(tick.tradingDay())
						.updateDate(tick.actionDay())
						.updateTime(tick.actionTime())
						.direction(req.direction())
						.offsetFlag(req.offsetFlag())
						.orderPriceType(req.price() == 0 ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
						.orderStatus(OrderStatusEnum.OS_AllTraded)
						.statusMsg("全部成交")
						.totalVolume(req.totalVolume())
						.tradedVolume(req.totalVolume())
						.price(tick.lastPrice())
						.build());
				
				onTrade(Trade.builder()
						.gatewayId(getId())
						.originOrderId(req.originOrderId())
						.orderId(orderId)
						.tradeId(UUID.randomUUID().toString())
						.contract(tick.contract())
						.tradingDay(tick.tradingDay())
						.tradeDate(tick.actionDay())
						.tradeTime(tick.actionTime())
						.tradeTimestamp(tick.actionTimestamp())
						.direction(req.direction())
						.offsetFlag(req.offsetFlag())
						.volume(req.totalVolume())
						.price(tick.lastPrice())
						.build());
				finishedSet.add(req.originOrderId());
			}
		});
		
		finishedSet.forEach(id -> {
			pendingClose.remove(id);
			pendingOpen.remove(id);
			pendingOrders.remove(id);
		});
	}
	

}
