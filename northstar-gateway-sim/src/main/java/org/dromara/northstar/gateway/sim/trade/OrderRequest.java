package org.dromara.northstar.gateway.sim.trade;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.utils.FieldUtils;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public class OrderRequest implements TickDataAware{

	private Consumer<Transaction> onTradeCallback;
	
	private Consumer<OrderField> onOrderCallback;
	
	private SubmitOrderReqField submitOrderReq;
	
	private int tradedVolume;
	
	private SimGatewayAccount account;
	
	private OrderField.Builder orderBuilder;
	
	public OrderRequest(SimGatewayAccount account, SubmitOrderReqField submitOrderReq, 
			Consumer<OrderField> onOrderCallback, Consumer<Transaction> onTradeCallback) {
		this.account = account;
		this.submitOrderReq = submitOrderReq;
		this.onOrderCallback = onOrderCallback;
		this.onTradeCallback = onTradeCallback;
		this.orderBuilder = OrderField.newBuilder()
				.setActiveTime(String.valueOf(System.currentTimeMillis()))
				.setOrderId(submitOrderReq.getGatewayId() + "_" + UUID.randomUUID().toString())
				.setContract(submitOrderReq.getContract())
				.setPrice(submitOrderReq.getPrice())
				.setDirection(submitOrderReq.getDirection())
				.setOriginOrderId(submitOrderReq.getOriginOrderId())
				.setGatewayId(submitOrderReq.getGatewayId())
				.setVolumeCondition(submitOrderReq.getVolumeCondition())
				.setOrderDate(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setOrderTime(LocalTime.now().format(DateTimeConstant.T_FORMAT_FORMATTER))
				.setAccountId(submitOrderReq.getGatewayId())
				.setTotalVolume(submitOrderReq.getVolume())
				.setOffsetFlag(submitOrderReq.getOffsetFlag())
				.setOrderPriceType(submitOrderReq.getOrderPriceType())
				.setGtdDate(submitOrderReq.getGtdDate())
				.setMinVolume(submitOrderReq.getMinVolume())
				.setStopPrice(submitOrderReq.getStopPrice())
				.setSequenceNo("1");
	}
	
	public String originOrderId() {
		return submitOrderReq.getOriginOrderId();
	}
	
	@Override
	public void onTick(TickField tick) {
		if(!StringUtils.equals(submitOrderReq.getContract().getUnifiedSymbol(), tick.getUnifiedSymbol())) {
			return;
		}
		if(hasDone())	
			return; 
		synchronized(this) {
			if(submitOrderReq.getOrderPriceType() == OrderPriceTypeEnum.OPT_AnyPrice
					|| FieldUtils.isSell(submitOrderReq.getDirection()) && tick.getBidPrice(0) >= submitOrderReq.getPrice()
					|| FieldUtils.isBuy(submitOrderReq.getDirection()) && tick.getAskPrice(0) <= submitOrderReq.getPrice()) {
				onOrderCallback.accept(orderBuilder
						.setTradedVolume(submitOrderReq.getVolume())
						.setTradingDay(tick.getTradingDay())
						.setStatusMsg("全部成交")
						.setOrderStatus(OrderStatusEnum.OS_AllTraded)
						.build());
				
				onTradeCallback.accept(Transaction.builder()
						.dealTick(tick)
						.orderReq(submitOrderReq)
						.build());
				
				tradedVolume = submitOrderReq.getVolume();
			}
		}
	}
	
	public synchronized boolean validate() {
		if(submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_Unknown) {
			throw new IllegalStateException("非法委托操作");
		}
		if(submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
			double cost = cost();
			double available = account.available();
			if(cost > available) {
				log.warn("可用资金不足，无法开仓。可用：{}，实际开仓：{}", available, cost);
				orderBuilder.setStatusMsg("废单").setOrderStatus(OrderStatusEnum.OS_Rejected);
			} else {
				log.info("成功下单：{}，{}，{}，{}手，委托价：{}，订单ID：{}", 
						submitOrderReq.getContract().getName(), submitOrderReq.getDirection(), submitOrderReq.getOffsetFlag(), 
						submitOrderReq.getVolume(), submitOrderReq.getPrice(), submitOrderReq.getOriginOrderId());
				orderBuilder.setStatusMsg("已报单").setOrderStatus(OrderStatusEnum.OS_Unknown);
			}
		} else {
			int availablePos = account.getPositionManager().getAvailablePosition(submitOrderReq.getDirection(), submitOrderReq.getContract().getUnifiedSymbol(), true);
			if(submitOrderReq.getVolume() > availablePos) {
				log.warn("可用持仓不足，无法平仓。可用：{}，实际平仓：{}", availablePos, submitOrderReq.getVolume());
				orderBuilder.setStatusMsg("废单").setOrderStatus(OrderStatusEnum.OS_Rejected);
			} else {
				log.info("成功下单：{}，{}，{}，{}手，委托价：{}，订单ID：{}", 
						submitOrderReq.getContract().getName(), submitOrderReq.getDirection(), submitOrderReq.getOffsetFlag(), 
						submitOrderReq.getVolume(), submitOrderReq.getPrice(), submitOrderReq.getOriginOrderId());
				orderBuilder.setStatusMsg("已报单").setOrderStatus(OrderStatusEnum.OS_Unknown);
			}
		}
		
		OrderField order = orderBuilder.build();
		onOrderCallback.accept(order);
		return order.getOrderStatus() != OrderStatusEnum.OS_Rejected;
	}
	
	public boolean hasDone() {
		return submitOrderReq.getVolume() == tradedVolume;
	}

	public Type orderType() {
		return submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_Open ? Type.OPEN : Type.CLOSE;
	}
	
	/**
	 * 订单占用资金
	 * @return
	 */
	public double cost() {
		ContractField contract = submitOrderReq.getContract();
		int vol = submitOrderReq.getVolume();
		double multipler = contract.getMultiplier();
		double price = submitOrderReq.getPrice();
		double marginRatio = FieldUtils.isBuy(submitOrderReq.getDirection()) ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		return price * vol * multipler * marginRatio;
	}
	
	public int totalVolume() {
		return submitOrderReq.getVolume();
	}
	
	public int pendingVolume() {
		return totalVolume() - tradedVolume;
	}
	
	public enum Type {
		OPEN,
		CLOSE;
	}
}
