package tech.xuanwu.northstar.gateway.sim.trade;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.constant.DateTimeConstant;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 此对象是一个交易意图对象，用于管理一个交易意图的整个生命周期 从委托请求 => 订单 => 成交（持仓） => 止损
 * 
 * @author KevinHuangwl
 *
 */
@Slf4j
public class TradeIntent {

	private SubmitOrderReqField orderReq;

	private OrderField.Builder order = OrderField.newBuilder();

	private TradeField.Builder trade = TradeField.newBuilder();
	
	private ConcurrentHashMap<String, TradeIntent> intentMap;
	
	// 0:委托请求，1:订单，2:成交(持仓)，3:止损，9:终结
	private static final int STATE_REQ = 0;
	private static final int STATE_ORDER = 1;
	private static final int STATE_TRADED = 2;
	private static final int STATE_HOLDING = 3;
	private static final int STATE_STOP_TRADED = 4;
	private static final int STATE_TERMINATED = 9;
	private AtomicInteger state = new AtomicInteger(0);

	public TradeIntent(SubmitOrderReqField orderReq, ConcurrentHashMap<String, TradeIntent> intentMap) {
		this.orderReq = orderReq;
		this.intentMap = intentMap;
	}

	public OrderField transformOrder(String tradingDay) {
		if (state.get() != STATE_REQ) {
			throw new IllegalStateException("当前状态不匹配：" + state.get());
		}
		state.incrementAndGet();
		OffsetFlagEnum offsetFlag = orderReq.getOffsetFlag() != OffsetFlagEnum.OF_Open && orderReq.getOffsetFlag() != OffsetFlagEnum.OF_Unknown 
				? OffsetFlagEnum.OF_Close 
				: orderReq.getOffsetFlag();
		return order.setActiveTime(String.valueOf(System.currentTimeMillis()))
				.setOrderId(orderReq.getGatewayId() + "_" + UUID.randomUUID().toString())
				.setContract(orderReq.getContract())
				.setPrice(orderReq.getPrice())
				.setDirection(orderReq.getDirection())
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setGatewayId(orderReq.getGatewayId())
				.setVolumeCondition(orderReq.getVolumeCondition())
				.setTradingDay(tradingDay)
				.setOrderDate(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setOrderTime(LocalTime.now().format(DateTimeConstant.T_FORMAT_FORMATTER))
				.setAccountId(orderReq.getGatewayId())
				.setTotalVolume(orderReq.getVolume())
				.setOffsetFlag(offsetFlag)
				.setOrderPriceType(orderReq.getOrderPriceType())
				.setGtdDate(orderReq.getGtdDate())
				.setMinVolume(orderReq.getMinVolume())
				.setStopPrice(orderReq.getStopPrice())
				.setSequenceNo("1")
				.setOrderStatus(OrderStatusEnum.OS_Touched)
				.setStatusMsg("已报单")
				.build();
	}
	
	public OrderField rejectOrder(String tradingDay, String message) {
		if (state.get() != STATE_REQ) {
			throw new IllegalStateException("当前状态不匹配：" + state.get());
		}
		terminated();
		return order.setActiveTime(String.valueOf(System.currentTimeMillis()))
				.setOrderId(orderReq.getGatewayId() + "_" + UUID.randomUUID().toString())
				.setContract(orderReq.getContract())
				.setPrice(orderReq.getPrice())
				.setDirection(orderReq.getDirection())
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setGatewayId(orderReq.getGatewayId())
				.setVolumeCondition(orderReq.getVolumeCondition())
				.setTradingDay(tradingDay)
				.setOrderDate(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setOrderTime(LocalTime.now().format(DateTimeConstant.T_FORMAT_FORMATTER))
				.setAccountId(orderReq.getGatewayId())
				.setTotalVolume(orderReq.getVolume())
				.setOffsetFlag(orderReq.getOffsetFlag())
				.setOrderPriceType(orderReq.getOrderPriceType())
				.setGtdDate(orderReq.getGtdDate())
				.setMinVolume(orderReq.getMinVolume())
				.setStopPrice(orderReq.getStopPrice())
				.setSequenceNo("1")
				.setOrderStatus(OrderStatusEnum.OS_Rejected)
				.setStatusMsg(message)
				.build();
	}

	public OrderField cancelOrder() {
		if (state.get() != STATE_ORDER) {
			return order.build();
		}
		terminated();
		log.info("成功撤单：{}，合约：{}", order.getOriginOrderId(), order.getContract().getName());
		return order.setOrderStatus(OrderStatusEnum.OS_Canceled).setStatusMsg("已撤单").build();
	}

	public Optional<OrderField> tryDeal(TickField tick) {
		if(!isContractMatch(tick.getUnifiedSymbol())) {
			throw new IllegalArgumentException("合约不匹配");
		}
		ContractField contract = orderReq.getContract();
		if(state.get() == STATE_ORDER 
				&& (order.getDirection() == DirectionEnum.D_Buy && tick.getAskPrice(0) <= order.getPrice()
				|| order.getDirection() == DirectionEnum.D_Sell && tick.getBidPrice(0) >= order.getPrice())) {			
			state.incrementAndGet();
			trade = TradeField.newBuilder()
					.setTradeId(System.currentTimeMillis()+"")
					.setAccountId(orderReq.getGatewayId())
					.setAdapterOrderId("")
					.setContract(contract)
					.setDirection(order.getDirection())
					.setGatewayId(orderReq.getGatewayId())
					.setHedgeFlag(order.getHedgeFlag())
					.setOffsetFlag(order.getOffsetFlag())
					.setOrderId(order.getOrderId())
					.setOriginOrderId(order.getOriginOrderId())
					.setPrice(order.getDirection() == DirectionEnum.D_Buy ? tick.getAskPrice(0) : tick.getBidPrice(0))
					.setPriceSource(PriceSourceEnum.PSRC_LastPrice)
					.setTradeDate(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
					.setTradingDay(tick.getTradingDay())
					.setTradeTime(LocalTime.now().format(DateTimeConstant.T_FORMAT_FORMATTER))
					.setVolume(order.getTotalVolume());
			return Optional.of(order
					.setTradedVolume(order.getTotalVolume())
					.setOrderStatus(OrderStatusEnum.OS_AllTraded)
					.setStatusMsg("挂单全部成交")
					.build());
		}
		return Optional.empty();
	}
	
	public TradeField transformTrade() {
		if(state.get() == STATE_STOP_TRADED) {
			terminated();
			return trade.build();
		}
		if(state.get() != STATE_TRADED) {
			throw new IllegalStateException("当前状态不匹配：" + state.get());
		}
		state.incrementAndGet();
		return trade.build();
	}

	public double getFrozenMargin() {
		if(isOpen() && state.get() == STATE_ORDER) {
			return (order.getTotalVolume() - order.getTradedVolume()) * order.getContract().getMultiplier() * order.getPrice() * order.getContract().getLongMarginRatio();
		}
		return 0;
	}
	
	public boolean isTerminated() {
		return state.get() == STATE_TERMINATED;
	}
	
	public boolean isOpen() {
		return orderReq.getOffsetFlag() == OffsetFlagEnum.OF_Open;
	}
	
	private void terminated() {
		intentMap.remove(orderReq.getOriginOrderId());
		state.set(STATE_TERMINATED);
	}
	
	public boolean isContractMatch(String unifiedSymbol) {
		return StringUtils.equals(unifiedSymbol, orderReq.getContract().getUnifiedSymbol());
	}
}
