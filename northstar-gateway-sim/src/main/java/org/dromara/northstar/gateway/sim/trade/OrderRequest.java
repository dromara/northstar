package org.dromara.northstar.gateway.sim.trade;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.FieldUtils;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;

@Slf4j
public class OrderRequest implements TickDataAware{

	private Consumer<Transaction> onTradeCallback;
	
	private Consumer<Order> onOrderCallback;
	
	private SubmitOrderReq submitOrderReq;
	
	private int tradedVolume;
	
	private SimGatewayAccount account;
	
	private Order orderTemplate;
	
	private boolean hasCancelled;
	
	public OrderRequest(SimGatewayAccount account, SubmitOrderReq submitOrderReq, 
			Consumer<Order> onOrderCallback, Consumer<Transaction> onTradeCallback) {
		this.account = account;
		this.submitOrderReq = submitOrderReq;
		this.onOrderCallback = onOrderCallback;
		this.onTradeCallback = onTradeCallback;
		this.orderTemplate = Order.builder()
				.orderId(submitOrderReq.gatewayId() + "_" + UUID.randomUUID().toString())
				.contract(submitOrderReq.contract())
				.price(submitOrderReq.price())
				.direction(submitOrderReq.direction())
				.originOrderId(submitOrderReq.originOrderId())
				.gatewayId(submitOrderReq.gatewayId())
				.updateDate(LocalDate.now())
				.updateTime(LocalTime.now())
				.totalVolume(submitOrderReq.volume())
				.offsetFlag(submitOrderReq.offsetFlag())
				.orderPriceType(submitOrderReq.orderPriceType())
				.gtdDate(submitOrderReq.gtdDate())
				.minVolume(submitOrderReq.minVolume())
				.stopPrice(submitOrderReq.stopPrice())
				.build();
	}
	
	public String originOrderId() {
		return submitOrderReq.originOrderId();
	}
	
	@Override
	public void onTick(Tick tick) {
		if(!StringUtils.equals(submitOrderReq.contract().unifiedSymbol(), tick.contract().unifiedSymbol())) {
			return;
		}
		if(hasDone())	
			return; 
		synchronized(this) {
			if(hasDone()) {
				return;
			}
			if(submitOrderReq.orderPriceType() == OrderPriceTypeEnum.OPT_AnyPrice
					|| FieldUtils.isSell(submitOrderReq.direction()) && tick.bidPrice().get(0) >= submitOrderReq.price()
					|| FieldUtils.isBuy(submitOrderReq.direction()) && tick.askPrice().get(0) <= submitOrderReq.price()) {
				onOrderCallback.accept(orderTemplate.toBuilder()
						.tradedVolume(submitOrderReq.volume())
						.tradingDay(tick.tradingDay())
						.statusMsg("全部成交")
						.orderStatus(OrderStatusEnum.OS_AllTraded)
						.build());
				
				onTradeCallback.accept(Transaction.builder()
						.dealTick(tick)
						.orderReq(submitOrderReq)
						.build());
				
				tradedVolume = submitOrderReq.volume();
			}
		}
	}
	
	public synchronized boolean validate() {
		if(submitOrderReq.offsetFlag() == OffsetFlagEnum.OF_Unknown) {
			throw new IllegalStateException("非法委托操作");
		}
		if(submitOrderReq.offsetFlag() == OffsetFlagEnum.OF_Open) {
			double cost = cost();
			double available = account.available();
			if(cost > available) {
				log.warn("[{}] 可用资金不足，无法开仓。可用：{}，实际开仓：{}", submitOrderReq.gatewayId(), available, cost);
				orderTemplate = orderTemplate.toBuilder().statusMsg("废单").orderStatus(OrderStatusEnum.OS_Rejected).build();
			} else {
				log.info("[{}] 成功下单：{}，{}，{}，{}手，委托价：{}，订单ID：{}", submitOrderReq.gatewayId(),
						submitOrderReq.contract().name(), submitOrderReq.direction(), submitOrderReq.offsetFlag(),
						submitOrderReq.volume(), submitOrderReq.price(), submitOrderReq.originOrderId());
				orderTemplate = orderTemplate.toBuilder().statusMsg("已报单").orderStatus(OrderStatusEnum.OS_Unknown).build();
			}
		} else {
			DirectionEnum posDir = FieldUtils.getOpposite(submitOrderReq.direction());
			int availablePos = account.getPositionManager().getAvailablePosition(posDir, submitOrderReq.contract());
			if(submitOrderReq.volume() > availablePos) {
				log.warn("[{}] 可用持仓不足，无法平仓。可用：{}，实际平仓：{}", submitOrderReq.gatewayId(), availablePos, submitOrderReq.volume());
				orderTemplate = orderTemplate.toBuilder().statusMsg("废单").orderStatus(OrderStatusEnum.OS_Rejected).build();
			} else {
				log.info("[{}] 成功下单：{}，{}，{}，{}手，委托价：{}，订单ID：{}", submitOrderReq.gatewayId(),
						submitOrderReq.contract().name(), submitOrderReq.direction(), submitOrderReq.offsetFlag(),
						submitOrderReq.volume(), submitOrderReq.price(), submitOrderReq.originOrderId());
				orderTemplate = orderTemplate.toBuilder().statusMsg("已报单").orderStatus(OrderStatusEnum.OS_Unknown).build();
			}
		}
		
		onOrderCallback.accept(orderTemplate);
		return orderTemplate.orderStatus() != OrderStatusEnum.OS_Rejected;
	}
	
	public boolean hasDone() {
		return hasCancelled || submitOrderReq.volume() == tradedVolume;
	}

	public Type orderType() {
		return submitOrderReq.offsetFlag() == OffsetFlagEnum.OF_Open ? Type.OPEN : Type.CLOSE;
	}
	
	public void cancelOrder() {
		onOrderCallback.accept(orderTemplate.toBuilder().statusMsg("已撤单")
										.orderStatus(OrderStatusEnum.OS_Canceled)
										.updateTime(LocalTime.now())
										.build());
		hasCancelled = true;
	}
	
	/**
	 * 订单占用资金
	 * @return
	 */
	public double cost() {
		Contract contract = submitOrderReq.contract();
		int vol = submitOrderReq.volume();
		double multiplier = contract.multiplier();
		double price = submitOrderReq.price();
		double marginRatio = FieldUtils.isBuy(submitOrderReq.direction()) ? contract.longMarginRatio() : contract.shortMarginRatio();
		return price * vol * multiplier * marginRatio;
	}
	
	public int totalVolume() {
		return submitOrderReq.volume();
	}
	
	public int pendingVolume() {
		return totalVolume() - tradedVolume;
	}
	
	public enum Type {
		OPEN,
		CLOSE;
	}
}
