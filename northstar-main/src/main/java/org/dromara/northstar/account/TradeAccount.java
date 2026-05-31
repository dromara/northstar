package org.dromara.northstar.account;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

import org.dromara.northstar.common.exception.IllegalOrderRequestException;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.strategy.IAccount;
import org.springframework.util.StringUtils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.AtomicDouble;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;


/**
 * 交易账户
 * 一个实例对应一个真实的交易账户
 * 主要用于方便聚合账户的实时状态，比如持仓查询、账户查询；也可以提前做发单过滤，比如开仓时资金够不够，平仓时数量对不对
 * @author KevinHuangwl
 *
 */
@Slf4j
public class TradeAccount implements IAccount {
	
	private TradeGateway gateway;

	/* 持仓信息 */
	private Table<PositionDirectionEnum, Contract, Position> posTable = HashBasedTable.create();
	
	/* 账户信息 */
	private Account accountState;
	
	/* 成交信息 */
	private Queue<Trade> tradeRecords = new LinkedList<>();
	
	/* 挂单信息 */
	private Map<String, Order> pendingOrderMap = new HashMap<>();
	
	/* 订单信息 */
	private Queue<Order> orderRecords = new LinkedList<>();
	
	/* 本地资金锁定 */
	protected Map<String, SubmitOrderReq> pendingOrderReqMap = new HashMap<>();
	protected Map<SubmitOrderReq, Double> pendingSubmitReqMap = new HashMap<>();
	
	public TradeAccount(TradeGateway gateway) {
		this.gateway = gateway;
	}

	@Override
	public synchronized void onOrder(Order order) {
		log.info("[{}] 收到网关订单反馈：[{}] {} {}", getId(), order.originOrderId(), order.orderStatus(), order.statusMsg());
		orderRecords.add(order);
		if(OrderUtils.isDoneOrder(order)) {
			pendingOrderMap.remove(order.originOrderId());
		} else if(OrderUtils.isPendingOrder(order)) {
			pendingOrderMap.put(order.originOrderId(), order);
		}
		
		if(StringUtils.hasText(order.originOrderId())) {
			SubmitOrderReq orderReq = pendingOrderReqMap.remove(order.originOrderId());
			if(orderReq != null) {
				pendingSubmitReqMap.remove(orderReq);
			}
		}
	}

	@Override
	public synchronized void onTrade(Trade trade) {
		log.info("[{}] 收到网关成交反馈：[{}] 数量：{}", getId(), trade.originOrderId(), trade.volume());
		tradeRecords.add(trade);
	}

	@Override
	public synchronized void submitOrder(SubmitOrderReq orderReq) {
		log.info("[{}] 收到委托请求 [{}]", getId(), orderReq.originOrderId());
		if(FieldUtils.isOpen(orderReq.offsetFlag())) {
			checkIfHasSufficientAmount(orderReq);
		} else {
			checkIfHasSufficientPosition(orderReq);
		}
		getTradeGateway().submitOrder(orderReq);
	}
	
	private void checkIfHasSufficientPosition(SubmitOrderReq submitOrder) {
		Contract contract =  submitOrder.contract();
		Optional<Position> posOpt = getPosition(FieldUtils.closePositionDirOf(submitOrder.direction()), contract);
		AtomicDouble availablePos = new AtomicDouble();
		posOpt.ifPresent(p -> availablePos.set(p.position() - p.frozen()));
		if(availablePos.get() < submitOrder.volume()) {
			log.warn("[{}] 持仓不够。可用持仓为：{}，委托交易量：{}", getId(), availablePos.get(), submitOrder.volume());
			throw new IllegalOrderRequestException();
		}
	}

	private void checkIfHasSufficientAmount(SubmitOrderReq submitOrder) {
		double margin = submitOrder.contract().contractDefinition().marginRatio();
		double cost = submitOrder.price() * submitOrder.volume() * submitOrder.contract().multiplier() * margin;
		if(availableBalance() < cost) {
			log.warn("[{}] 资金不足。可用资金为：{}，开仓成本为：{}", getId(), availableBalance(), cost);
			throw new IllegalOrderRequestException();
		}
		
		pendingSubmitReqMap.put(submitOrder, cost);
		pendingOrderReqMap.put(submitOrder.originOrderId(), submitOrder);
	}

	@Override
	public synchronized boolean cancelOrder(CancelOrderReq cancelReq) {
		log.info("[{}] 收到撤单请求 [{}]", getId(), cancelReq.originOrderId());
		if(!pendingOrderMap.containsKey(cancelReq.originOrderId())) {
			log.info("[{}] 订单[{}]已经不存在", getId(), cancelReq.originOrderId());
			return false;
		}
		return getTradeGateway().cancelOrder(cancelReq);
	}

	@Override
	public synchronized double accountBalance() {
		if(Objects.isNull(accountState))
			return 0;
		return accountState.balance();
	}

	@Override
	public synchronized double availableBalance() {
		if(Objects.isNull(accountState))
			return 0;
		return accountBalance() - totalLocalFrozen();
	}
	
	private double totalLocalFrozen() {
		return pendingSubmitReqMap.values().stream().mapToDouble(Double::doubleValue).sum();
	}

	@Override
	public String getId() {
		return gateway.getDescription().getGatewayId();
	}

	@Override
	public TradeGateway getTradeGateway() {
		return gateway;
	}
	
	public synchronized Optional<Position> getPosition(PositionDirectionEnum posDirection, Contract contract) {
		if(!posTable.contains(posDirection, contract)) {
			return Optional.empty();
		}
		return Optional.of(posTable.get(posDirection, contract));
	}

	@Override
	public List<Order> getPendingOrders() {
		return pendingOrderMap.values().stream().toList();
	}

	public void onAccount(Account account) {
		accountState = account;
	}

	public synchronized void onPosition(Position position) {
		posTable.put(position.positionDirection(), position.contract(), position);
	}
}
