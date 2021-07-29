package tech.xuanwu.northstar.gateway.sim;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.constant.DateTimeConstant;
import tech.xuanwu.northstar.common.model.ContractManager;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
class GwOrderHolder {
	
	private String gatewayId;
	
	private ContractManager contractMgr;
	
	private int ticksOfCommission;
	
	/**
	 * orderId --> order
	 */
	private ConcurrentHashMap<String, OrderField> orderIdMap = new ConcurrentHashMap<>(100);
	/**
	 * originOrderId --> orderId
	 */
	private ConcurrentHashMap<String, String> originOrderIdMap = new ConcurrentHashMap<>(100);
	private ConcurrentHashMap<TradeField, OrderField> doneOrderMap = new ConcurrentHashMap<>();
	/**
	 * unifiedSymbol --> tradingDay
	 */
	private final Map<String,String> tradingDayMap = new HashMap<>(1000);
	
	public GwOrderHolder (String gatewayId, int ticksOfCommission, ContractManager contractMgr) {
		this.gatewayId = gatewayId;
		this.ticksOfCommission = ticksOfCommission;
		this.contractMgr = contractMgr;
	}
	
	private OrderField.Builder makeOrder(SubmitOrderReqField submitOrderReq){
		String orderId = gatewayId + "_" + UUID.randomUUID().toString();
		String originOrderId = submitOrderReq.getOriginOrderId();
		OrderField.Builder ob = OrderField.newBuilder();
		ob.setActiveTime(String.valueOf(System.currentTimeMillis()));
		ob.setOrderId(orderId);
		ob.setContract(submitOrderReq.getContract());
		ob.setPrice(submitOrderReq.getPrice());
		ob.setDirection(submitOrderReq.getDirection());
		ob.setOriginOrderId(originOrderId);
		ob.setGatewayId(gatewayId);
		ob.setVolumeCondition(submitOrderReq.getVolumeCondition());
		ob.setTradingDay(Optional.ofNullable(tradingDayMap.get(submitOrderReq.getContract().getUnifiedSymbol())).orElse(""));
		ob.setOrderDate(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		ob.setOrderTime(LocalTime.now().format(DateTimeConstant.T_FORMAT_FORMATTER));
		ob.setAccountId(gatewayId);
		ob.setTotalVolume(submitOrderReq.getVolume());
		ob.setOffsetFlag(submitOrderReq.getOffsetFlag());
		ob.setOrderPriceType(submitOrderReq.getOrderPriceType());
		ob.setGtdDate(submitOrderReq.getGtdDate());
		ob.setMinVolume(submitOrderReq.getMinVolume());
		ob.setStopPrice(submitOrderReq.getStopPrice());
		ob.setSequenceNo("1");
		ob.setOrderStatus(OrderStatusEnum.OS_Touched);
		ob.setStatusMsg("报单已提交");
		
		return ob;
	}
	
	protected OrderField tryOrder(SubmitOrderReqField submitOrderReq, AccountField af) {
		OrderField.Builder ob = makeOrder(submitOrderReq);
		ContractField contract = submitOrderReq.getContract();
		double marginRate = submitOrderReq.getDirection() == DirectionEnum.D_Buy ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		int vol = submitOrderReq.getVolume();
		double price = submitOrderReq.getPrice();
		double cost = vol * price * contract.getMultiplier() * marginRate + contract.getPriceTick() * ticksOfCommission;
		if(cost > af.getAvailable()) {
			ob.setOrderStatus(OrderStatusEnum.OS_Rejected);
			ob.setStatusMsg("资金不足");
			log.warn("资金不足，无法下单。当前可用资金：{}，下单成本：{}，订单：{}", af.getAvailable(), cost, submitOrderReq);
			return ob.build();
		}
		
		OrderField of = ob.build();
		orderIdMap.put(of.getOrderId(), of);
		originOrderIdMap.put(of.getOriginOrderId(), of.getOrderId());
		log.info("成功下单：{}, {}, {}, {}, {}手, {}", of.getOriginOrderId(), of.getContract().getName(), of.getDirection(),
				of.getOffsetFlag(), of.getTotalVolume(), of.getPrice());
		return of;
	}
	
	protected OrderField tryOrder(SubmitOrderReqField submitOrderReq, PositionField pf) {
		OrderField.Builder ob = makeOrder(submitOrderReq);
		if(pf == null) {
			ob.setOrderStatus(OrderStatusEnum.OS_Rejected);
			ob.setStatusMsg("仓位不足");
			log.warn("仓位不足，无法下单：{}, {}, {}, {}, {}手, {}", submitOrderReq.getOriginOrderId(), submitOrderReq.getContract().getName(),
					submitOrderReq.getDirection(), submitOrderReq.getOffsetFlag(), submitOrderReq.getVolume(), submitOrderReq.getPrice());
			return ob.build();
		}
		int totalAvailable = pf.getPosition() - pf.getFrozen();
		int tdAvailable = pf.getTdPosition() - pf.getTdFrozen();
		int ydAvailable = pf.getYdPosition() - pf.getYdFrozen();
		if(submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_CloseToday && tdAvailable < submitOrderReq.getVolume()
				|| submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_CloseYesterday && ydAvailable < submitOrderReq.getVolume()
				|| totalAvailable < submitOrderReq.getVolume()) {
			ob.setOrderStatus(OrderStatusEnum.OS_Rejected);
			ob.setStatusMsg("仓位不足");
			log.warn("仓位不足，无法下单：{}, {}, {}, {}, {}手, {}", submitOrderReq.getOriginOrderId(), submitOrderReq.getContract().getName(),
					submitOrderReq.getDirection(), submitOrderReq.getOffsetFlag(), submitOrderReq.getVolume(), submitOrderReq.getPrice());
			return ob.build();
		}
		
		OrderField of = ob.build();
		orderIdMap.put(of.getOrderId(), of);
		originOrderIdMap.put(of.getOriginOrderId(), of.getOrderId());
		log.info("成功下单：{}, {}, {}, {}, {}手, {}", of.getOriginOrderId(), of.getContract().getName(), of.getDirection(),
				of.getOffsetFlag(), of.getTotalVolume(), of.getPrice());
		return of;
	}
	
	protected OrderField cancelOrder(CancelOrderReqField cancelOrderReq) {
		OrderField order = null;
		if(StringUtils.isEmpty(cancelOrderReq.getOrderId()) && StringUtils.isEmpty(cancelOrderReq.getOriginOrderId())) {
			throw new IllegalArgumentException("未提供要撤单的订单号");
		} else if(StringUtils.isNotEmpty(cancelOrderReq.getOrderId())) {
			order = orderIdMap.remove(cancelOrderReq.getOrderId());
			originOrderIdMap.remove(order.getOriginOrderId());
			
		} else if(StringUtils.isNotEmpty(cancelOrderReq.getOriginOrderId())) {
			String orderId = originOrderIdMap.remove(cancelOrderReq.getOriginOrderId());
			order = orderIdMap.remove(orderId);
			
		}
		
		// TODO 已成交的订单还会不会在orderIdMap中？暂时假设不会
		OrderField.Builder ob = order.toBuilder();
		ob.setOrderStatus(OrderStatusEnum.OS_Canceled);
		ob.setCancelTime(LocalDateTime.now().format(DateTimeConstant.DT_FORMAT_FORMATTER));
		log.info("成功撤单：{}，合约：{}", ob.getOrderId(), ob.getContract().getName());
		return ob.build();
	}
	
	protected List<TradeField> tryDeal(TickField tick) {
		tradingDayMap.put(tick.getUnifiedSymbol(), tick.getTradingDay());
		final String unifiedSymbol = tick.getUnifiedSymbol();
		List<TradeField> tradeList = new ArrayList<>();
		orderIdMap.forEach((k, order) -> {
			boolean untrade = order.getOrderStatus() != OrderStatusEnum.OS_AllTraded 
					&& order.getOrderStatus() != OrderStatusEnum.OS_Canceled
					&& order.getOrderStatus() != OrderStatusEnum.OS_Rejected;
			if(StringUtils.equals(order.getContract().getUnifiedSymbol(), unifiedSymbol) && untrade) {
				// TODO 该模拟撮合逻辑属于简单实现，没有考虑价格深度
				if(order.getDirection() == DirectionEnum.D_Buy && tick.getAskPrice(0) <= order.getPrice()
						|| order.getDirection() == DirectionEnum.D_Sell && tick.getBidPrice(0) >= order.getPrice()) {
					// 修改挂单状态
					OrderField.Builder ob = order.toBuilder();
					ob.setTradedVolume(ob.getTotalVolume());
					ob.setOrderStatus(OrderStatusEnum.OS_AllTraded);
					ob.setStatusMsg("挂单全部成交");
					
					OrderField of = ob.build();
					
					ContractField contract = contractMgr.getContract(unifiedSymbol);
					// 计算成交
					TradeField trade = TradeField.newBuilder()
							.setTradeId(System.currentTimeMillis()+"")
							.setAccountId(gatewayId)
							.setAdapterOrderId("")
							.setContract(contract)
							.setDirection(order.getDirection())
							.setGatewayId(gatewayId)
							.setHedgeFlag(order.getHedgeFlag())
							.setOffsetFlag(order.getOffsetFlag())
							.setOrderId(order.getOrderId())
							.setOriginOrderId(order.getOriginOrderId())
							.setPrice(order.getDirection() == DirectionEnum.D_Buy ? tick.getAskPrice(0) : tick.getBidPrice(0))
							.setPriceSource(PriceSourceEnum.PSRC_LastPrice)
							.setTradeDate(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
							.setTradingDay(Optional.ofNullable(tradingDayMap.get(unifiedSymbol)).orElse(""))
							.setTradeTime(LocalTime.now().format(DateTimeConstant.T_FORMAT_FORMATTER))
							.setVolume(order.getTotalVolume())
							.build();
					
					tradeList.add(trade);
					doneOrderMap.put(trade, of);
					orderIdMap.remove(of.getOrderId());
					
					log.info("模拟成交：{}，{}，{}，{}，{}手，{}，{}", trade.getOriginOrderId(), trade.getContract().getName(), 
							trade.getDirection(), trade.getOffsetFlag(), trade.getVolume(), trade.getPrice(), trade.getTradingDay());
				}
			}
		});
		
		return tradeList;
	}
	
	protected OrderField confirmWith(TradeField trade) {
		return doneOrderMap.remove(trade);
	}
	
	protected double getFrozenMargin() {
		double totalFrozenAmount = 0;
		Double r1 = orderIdMap.reduce(100, 
			(k, v) -> (v.getTotalVolume() - v.getTradedVolume()) * v.getContract().getMultiplier() * v.getPrice() * v.getContract().getLongMarginRatio(),
			(a, b) -> a + b);
		totalFrozenAmount += r1 == null ? 0 : r1.doubleValue();
		return totalFrozenAmount;
	}
	
	protected void proceedDailySettlement() {
		
	}
}
