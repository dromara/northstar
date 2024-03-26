package org.dromara.northstar.strategy.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.constant.PriceType;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;


/**
 * 交易意图
 * 封装自动撤单追单逻辑
 * @author KevinHuangwl
 *
 */
public class TradeIntent implements TransactionAware, TickDataAware {
	
	private IModuleContext context;
	/**
	 * 合约
	 */
	@Getter
	private final Contract contract;
	/**
	 * 操作
	 */
	@Getter
	private final SignalOperation operation;
	/**
	 * 价格类型
	 */
	@Getter
	private final PriceType priceType;
	/**
	 * 价格
	 */
	@Getter
	private final double price;
	/**
	 * 目标手数
	 */
	@Getter
	private final int volume;
	/**
	 * 订单超时（毫秒）
	 */
	private final long timeout;
	/**
	 * 价差过大的放弃条件
	 */
	private final Predicate<Double> priceDiffConditionToAbort;
	/**
	 * 意图初始价
	 */
	private Double initialPrice;
	
	private Logger logger;
	
	private Set<Trade> trades = new HashSet<>();
	
	@Builder
	public TradeIntent(Contract contract, SignalOperation operation, PriceType priceType, double price, int volume, 
			long timeout, Predicate<Double> priceDiffConditionToAbort) {
		Assert.noNullElements(List.of(contract, operation, priceType), "入参不能为空");
		Assert.isTrue(volume > 0, "手数必须为正整数");
		Assert.isTrue(timeout > 0, "订单等待时长必须为正整数");
		this.contract = contract;
		this.operation = operation;
		this.priceType = priceType;
		this.price = price;
		this.volume = volume;
		this.timeout = timeout;
		this.priceDiffConditionToAbort = priceDiffConditionToAbort;
	}
	
	private Optional<String> orderIdRef = Optional.empty();

	private int accVol;
	
	private boolean terminated;
	
	private long lastCancelReqTime;
	
	@Override
	public synchronized void onTick(Tick tick) {
		if(!contract.equals(tick.contract())) 
			return;
		
		if(Objects.isNull(initialPrice)) {
			initialPrice = price == 0 ? tick.lastPrice() : price;
			logger.debug("交易意图初始价位：{}", initialPrice);
		}
		if(Objects.nonNull(priceDiffConditionToAbort)) {
			double priceDiff = Math.abs(tick.lastPrice() - initialPrice);
			terminated = priceDiffConditionToAbort.test(priceDiff);
			if(terminated) {
				logger.info("{} {} 价差过大中止交易意图，当前价差为{}", tick.actionDay(), tick.actionTime(), priceDiff);
				orderIdRef.ifPresent(context::cancelOrder);
			}
		}
		if(hasTerminated()) {
			logger.debug("交易意图已终止");
			double avgDealPrice = trades.stream().mapToDouble(tr -> tr.price() * tr.volume()).sum() / volume;
			logger.info("交易滑点为：[{}]个价位", (int)(Math.abs(avgDealPrice - price) / contract.priceTick()));
			return;
		}
		if(orderIdRef.isEmpty() && !context.getState().isOrdering()) {
			logger.debug("交易意图自动发单");
			orderIdRef = context.submitOrderReq(contract, operation, priceType, restVol(), price);
		} else if (orderIdRef.isPresent() && context.isOrderWaitTimeout(orderIdRef.get(), timeout) && tick.actionTimestamp() - lastCancelReqTime > 3000) {
			logger.debug("交易意图自动撤单");
			context.cancelOrder(orderIdRef.get());
			lastCancelReqTime = tick.actionTimestamp();
			if(!context.isEnabled()) {
				terminated = true;	// 当模组停用后，交易意图自动终止
			}
		}
	}
	
	private int restVol() {
		return volume - accVol;
	}

	@Override
	public synchronized void onOrder(Order order) {
		// 订单结束
		orderIdRef
			.filter(id -> StringUtils.equals(id, order.originOrderId()))
			.ifPresent(id -> {
				if(OrderUtils.isDoneOrder(order)) {
					// 延时3秒再移除订单信息，避免移除了订单信息后，成交无法匹配的问题
					CompletableFuture.runAsync(() -> orderIdRef = Optional.empty(), CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS));
				}
			});
	}

	@Override
	public synchronized void onTrade(Trade trade) {
		orderIdRef
			.filter(id -> StringUtils.equals(id, trade.originOrderId()))
			.ifPresent(id -> {
				accVol += trade.volume();
				trades.add(trade);
			});
	}

	public synchronized boolean hasTerminated() {
		return terminated || accVol >= volume;
	}
	
	public void setContext(IModuleContext ctx) {
		this.context = ctx;
		this.logger = ctx.getLogger(getClass());
	}
	
	@Override
	public String toString() {
		return String.format("TradeIntent [contract=%s, operation=%s, priceType=%s, price=%s, volume=%s, timeout=%s]", 
				contract.contractId(), operation, priceType, price, volume, timeout);
	}

}
