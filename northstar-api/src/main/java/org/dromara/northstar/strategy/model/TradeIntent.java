package org.dromara.northstar.strategy.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.constant.PriceType;
import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;


/**
 * 交易意图
 * 封装自动撤单追单逻辑
 * @author KevinHuangwl
 *
 */
public class TradeIntent implements TransactionAware, TickDataAware {
	
	@Setter
	private IModuleContext context;
	/**
	 * 合约
	 */
	@Getter
	@NonNull
	private final ContractField contract;
	/**
	 * 操作
	 */
	@Getter
	@NonNull
	private final SignalOperation operation;
	/**
	 * 价格类型
	 */
	@NonNull
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
	
	private Double initialPrice;
	
	@Builder
	public TradeIntent(ContractField contract, SignalOperation operation, PriceType priceType, double price, int volume, 
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
	public synchronized void onTick(TickField tick) {
		if(!StringUtils.equals(tick.getUnifiedSymbol(), contract.getUnifiedSymbol())) 
			return;

		if(Objects.isNull(initialPrice)) {
			initialPrice = tick.getLastPrice();
		}
		if(Objects.nonNull(priceDiffConditionToAbort)) {
			double priceDiff = Math.abs(tick.getLastPrice() - initialPrice);
			terminated = priceDiffConditionToAbort.test(priceDiff);
			if(terminated) {
				context.getLogger().info("{} {} 价差中止条件已经满足，当前价差为{}", tick.getActionDay(), tick.getActionTime(), priceDiff);
			}
		}
		if(hasTerminated()) {
			context.getLogger().debug("交易意图已终止");
			return;
		}
		if(orderIdRef.isEmpty() && !context.getState().isOrdering()) {
			orderIdRef = context.submitOrderReq(contract, operation, priceType, volume - accVol, price);
		} else if (orderIdRef.isPresent() && context.isOrderWaitTimeout(orderIdRef.get(), timeout) && System.currentTimeMillis() - lastCancelReqTime > 3000) {
			context.cancelOrder(orderIdRef.get());
			lastCancelReqTime = System.currentTimeMillis();
		}
	}

	@Override
	public synchronized void onOrder(OrderField order) {
		// 订单结束
		orderIdRef
			.filter(id -> StringUtils.equals(id, order.getOriginOrderId()))
			.ifPresent(id -> {
				if(OrderUtils.isDoneOrder(order)) {	
					orderIdRef = Optional.empty();
				}
			});
	}

	@Override
	public synchronized void onTrade(TradeField trade) {
		accVol += trade.getVolume();
	}

	public boolean hasTerminated() {
		return terminated || accVol == volume;
	}

	@Override
	public String toString() {
		return String.format("TradeIntent [contract=%s, operation=%s, priceType=%s, price=%s, volume=%s, timeout=%s]", 
				contract.getContractId(), operation, priceType, price, volume, timeout);
	}
	
	
}
