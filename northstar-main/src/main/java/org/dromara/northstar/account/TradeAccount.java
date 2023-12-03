package org.dromara.northstar.account;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.strategy.IAccount;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;


/**
 * 交易账户
 * 一个实例对应一个真实的交易账户
 * @author KevinHuangwl
 *
 */
@Slf4j
public class TradeAccount implements IAccount {

	private TradeGateway tradeGateway;
	
	private MarketGateway marketGateway;
	
	private GatewayDescription gatewayDescription;
	
	/* 持仓信息 */
	private Table<PositionDirectionEnum, Contract, Position> posTable = HashBasedTable.create();
	
	/* 账户信息 */
	private Account accountField;
	
	/* 成交信息 */
	private Queue<Trade> tradeRecords = new LinkedList<>();
	
	/* 挂单信息 */
	private Map<String, Order> pendingOrderMap = new HashMap<>();
	
	/* 订单信息 */
	private Queue<Order> orderRecords = new LinkedList<>();
	
	/* 预锁定金额 */
	private Map<UUID, Double> frozenAmountMap = new HashMap<>();
	
	public TradeAccount(MarketGateway marketGateway, TradeGateway tradeGateway, GatewayDescription gatewayDescription) {
		this.marketGateway = marketGateway;
		this.tradeGateway = tradeGateway;
		this.gatewayDescription = gatewayDescription;
	}

	@Override
	public synchronized void onOrder(Order order) {
		if(OrderUtils.isDoneOrder(order)) {
			pendingOrderMap.remove(order.originOrderId());
			orderRecords.add(order);
		} else {
			pendingOrderMap.put(order.originOrderId(), order);
		}
	}

	@Override
	public synchronized void onTrade(Trade trade) {
		tradeRecords.add(trade);
	}

	@Override
	public String submitOrder(SubmitOrderReq orderReq) {
		log.info("[{}] 收到委托请求", accountId());
		return tradeGateway.submitOrder(orderReq);
	}

	@Override
	public boolean cancelOrder(String originOrderId) {
		return tradeGateway.cancelOrder(originOrderId);
	}

	@Override
	public synchronized double accountBalance() {
		if(Objects.isNull(accountField))
			return 0;
		return accountField.balance();
	}

	@Override
	public synchronized double availableAmount() {
		if(Objects.isNull(accountField))
			return 0;
		return accountField.available();
	}

	@Override
	public double degreeOfRisk() {
		if(accountBalance() == 0) 
			return 0;
		return 1 - availableAmount() / accountBalance();
	}

	@Override
	public synchronized Optional<UUID> tryLockAmount(double amount) {
		if(availableAmount() - totalFrozenAmount() - amount < 0) {
			return Optional.empty();
		}
		UUID uuid = UUID.randomUUID();
		frozenAmountMap.put(uuid, amount);
		return Optional.of(uuid);
	}
	
	private double totalFrozenAmount() {
		return frozenAmountMap.values().stream().mapToDouble(Double::doubleValue).sum();
	}

	@Override
	public synchronized void unlockAmount(UUID lockId) {
		frozenAmountMap.remove(lockId);
	}

	@Override
	public synchronized void onAccount(Account account) {
		accountField = account;
	}

	@Override
	public synchronized void onPosition(Position position) {
		posTable.put(position.positionDirection(), position.contract(), position);
	}

	@Override
	public String accountId() {
		return gatewayDescription.getGatewayId();
	}

	@Override
	public synchronized int netPosition(Contract contract) {
		int longPosition = posTable.contains(PositionDirectionEnum.PD_Long, contract) ? posTable.get(PositionDirectionEnum.PD_Long, contract).position() : 0;
		int shortPosition = posTable.contains(PositionDirectionEnum.PD_Short, contract) ? posTable.get(PositionDirectionEnum.PD_Short, contract).position() : 0;
		return longPosition - shortPosition;
	}

	@Override
	public MarketGateway getMarketGateway() {
		return marketGateway;
	}

	@Override
	public TradeGateway getTradeGateway() {
		return tradeGateway;
	}

	public synchronized Optional<Position> getPosition(PositionDirectionEnum posDirection, Contract contract) {
		if(!posTable.contains(posDirection, contract)) {
			return Optional.empty();
		}
		return Optional.of(posTable.get(posDirection, contract));
	}

}
