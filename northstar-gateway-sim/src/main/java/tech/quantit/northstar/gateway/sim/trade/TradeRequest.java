package tech.quantit.northstar.gateway.sim.trade;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.utils.FieldUtils;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 模拟交易请求
 * 管理交易请求生命周期 SubmitOrderReq -> Order -> Trade -> Position 
 * @author KevinHuangwl
 *
 */
@Slf4j
public abstract class TradeRequest implements TickDataAware, Cancellable {

	protected FastEventEngine feEngine;
	protected SubmitOrderReqField submitOrderReq;
	protected Consumer<TradeRequest> doneCallback;
	
	protected OrderField.Builder orderBuilder = OrderField.newBuilder();
	protected TradeField.Builder tradeBuilder = TradeField.newBuilder();
	
	protected TradeRequest(FastEventEngine feEngine, Consumer<TradeRequest> doneCallback) {
		this.feEngine = feEngine;
		this.doneCallback = doneCallback;
	}
	
	public String originOrderId() {
		return submitOrderReq.getOriginOrderId();
	}
	
	protected synchronized OrderField initOrder(SubmitOrderReqField orderReq) {
		this.submitOrderReq = orderReq;
		orderBuilder.setActiveTime(String.valueOf(System.currentTimeMillis()))
		.setOrderId(orderReq.getGatewayId() + "_" + UUID.randomUUID().toString())
		.setContract(orderReq.getContract())
		.setPrice(orderReq.getPrice())
		.setDirection(orderReq.getDirection())
		.setOriginOrderId(orderReq.getOriginOrderId())
		.setGatewayId(orderReq.getGatewayId())
		.setVolumeCondition(orderReq.getVolumeCondition())
		.setOrderDate(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
		.setOrderTime(LocalTime.now().format(DateTimeConstant.T_FORMAT_FORMATTER))
		.setAccountId(orderReq.getGatewayId())
		.setTotalVolume(orderReq.getVolume())
		.setOffsetFlag(orderReq.getOffsetFlag())
		.setOrderPriceType(orderReq.getOrderPriceType())
		.setGtdDate(orderReq.getGtdDate())
		.setMinVolume(orderReq.getMinVolume())
		.setStopPrice(orderReq.getStopPrice())
		.setSequenceNo("1");
		OrderField order;
		if(canMakeOrder()) {
			log.info("成功下单：{}, {}, {}, {}, {}手, 委托价：{}, 止损价：{}", submitOrderReq.getOriginOrderId(), submitOrderReq.getContract().getName(), submitOrderReq.getDirection(),
					submitOrderReq.getOffsetFlag(), submitOrderReq.getVolume(), submitOrderReq.getPrice(), submitOrderReq.getStopPrice());
			order = orderBuilder.setStatusMsg("已报单").setOrderStatus(OrderStatusEnum.OS_Unknown).build();
		} else {
			order = orderBuilder.setStatusMsg("废单").setOrderStatus(OrderStatusEnum.OS_Rejected).build();
		}
		feEngine.emitEvent(NorthstarEventType.ORDER, order);
		return order;
	}
	
	protected abstract boolean canMakeOrder();
	
	@Override
	public synchronized OrderField onCancal(CancelOrderReqField cancelReq) {
		if(!StringUtils.equals(submitOrderReq.getOriginOrderId(), cancelReq.getOriginOrderId())) {
			throw new IllegalArgumentException("撤单ID与委托单ID不一致：" + String.format("[%s] [%s]", submitOrderReq.getOriginOrderId(), cancelReq.getOriginOrderId()));
		}
		OrderField order = orderBuilder.setStatusMsg("已撤单").setOrderStatus(OrderStatusEnum.OS_Canceled).build();
		feEngine.emitEvent(NorthstarEventType.ORDER, order);
		doneCallback.accept(this);
		return order;
	}

	@Override
	public synchronized void onTick(TickField tick) {
		if(!StringUtils.equals(submitOrderReq.getContract().getUnifiedSymbol(), tick.getUnifiedSymbol())) {
			return;
		}
		if(submitOrderReq.getOrderPriceType() == OrderPriceTypeEnum.OPT_AnyPrice
				|| FieldUtils.isSell(submitOrderReq.getDirection()) && tick.getBidPrice(0) >= submitOrderReq.getPrice()
				|| FieldUtils.isBuy(submitOrderReq.getDirection()) && tick.getAskPrice(0) <= submitOrderReq.getPrice()) {
			OrderField order = orderBuilder
					.setTradedVolume(submitOrderReq.getVolume())
					.setTradingDay(tick.getTradingDay())
					.setStatusMsg("全部成交")
					.setOrderStatus(OrderStatusEnum.OS_AllTraded)
					.build();
			feEngine.emitEvent(NorthstarEventType.ORDER, order);
			
			double dealPrice = 0;
			if(submitOrderReq.getOrderPriceType() == OrderPriceTypeEnum.OPT_AnyPrice) {
				dealPrice = switch(order.getDirection()) {
					case D_Buy -> tick.getAskPrice(0);
					case D_Sell -> tick.getBidPrice(0);
					default -> throw new IllegalArgumentException("Unexpected value: " + order.getDirection());
				};
			} else {
				dealPrice = submitOrderReq.getPrice() > 0 ? submitOrderReq.getPrice() : tick.getLastPrice();
			}
			
			TradeField trade = tradeBuilder
					.setTradeId(System.currentTimeMillis()+"")
					.setAccountId(submitOrderReq.getGatewayId())
					.setAdapterOrderId("")
					.setContract(submitOrderReq.getContract())
					.setTradeTimestamp(tick.getActionTimestamp())
					.setDirection(order.getDirection())
					.setGatewayId(submitOrderReq.getGatewayId())
					.setHedgeFlag(order.getHedgeFlag())
					.setOffsetFlag(order.getOffsetFlag())
					.setOrderId(order.getOrderId())
					.setOriginOrderId(order.getOriginOrderId())
					.setPrice(dealPrice)
					.setPriceSource(PriceSourceEnum.PSRC_LastPrice)
					.setTradeDate(tick.getActionDay())
					.setTradingDay(tick.getTradingDay())
					.setTradeTime(tick.getActionTime())
					.setVolume(order.getTotalVolume())
					.build();
			log.info("模拟成交：{}，{}，{}，{}，{}手，{}，{}", trade.getOriginOrderId(), trade.getContract().getName(), 
					trade.getDirection(), trade.getOffsetFlag(), trade.getVolume(), trade.getPrice(), trade.getTradingDay());
			feEngine.emitEvent(NorthstarEventType.TRADE, trade);
			doneCallback.accept(this);
			onTrade(trade);
		}
	}
	
	public OrderField getOrder() {
		return orderBuilder.build();
	}
	
	public abstract void onTrade(TradeField trade);
}
