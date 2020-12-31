package tech.xuanwu.northstar.trader.domain.simulated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.constant.CommonConstant;
import xyz.redtorch.common.util.UUIDStringPoolUtils;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 *模拟盘订单计算
 * @author kevinhuangwl
 *
 */
@Slf4j
public class GwOrders {

	private volatile double commission;
	
	private ConcurrentHashMap<String, OrderField> orderIdMap = new ConcurrentHashMap<String, OrderField>(100);
	private ConcurrentHashMap<TradeField, OrderField> tradeOrderMap = new ConcurrentHashMap<>();
	
	private volatile String tradingDay = "";
	
	private GwAccount gwAccount;
	private GwPositions gwPositions;
	
	private Map<String, ContractField> contractMap;
	
	public void setGwAccount(GwAccount gwAccount) {
		this.gwAccount = gwAccount;
	}
	
	public void setGwPositions(GwPositions gwPositions) {
		this.gwPositions = gwPositions;
	}
	
	public void setContractMap(Map<String, ContractField> contractMap) {
		this.contractMap = contractMap;
	}
	
	public OrderField submitOrder(SubmitOrderReqField submitOrderReq) {
		String orderId = gwAccount.getAccount().getGatewayId() + CommonConstant.SIM_TAG + "_" + UUIDStringPoolUtils.getUUIDString();
		String originOrderId = submitOrderReq.getOriginOrderId();
		OrderField.Builder ob = OrderField.newBuilder();
		ob.setActiveTime(String.valueOf(System.currentTimeMillis()));
		ob.setOrderId(orderId);
		ob.setContract(submitOrderReq.getContract());
		ob.setPrice(submitOrderReq.getPrice());
		ob.setDirection(submitOrderReq.getDirection());
		ob.setOriginOrderId(originOrderId);
		ob.setGatewayId(gwAccount.getAccount().getGatewayId());
		ob.setVolumeCondition(submitOrderReq.getVolumeCondition());
		ob.setTradingDay(tradingDay);
		ob.setOrderDate(LocalDate.now().format(CommonConstant.D_FORMAT_INT_FORMATTER));
		ob.setOrderTime(LocalTime.now().format(CommonConstant.T_FORMAT_FORMATTER));
		ob.setAccountId(gwAccount.getAccount().getAccountId());
		ob.setTotalVolume(submitOrderReq.getVolume());
		ob.setOffsetFlag(submitOrderReq.getOffsetFlag());
		ob.setOrderPriceType(submitOrderReq.getOrderPriceType());
		ob.setGtdDate(submitOrderReq.getGtdDate());
		ob.setMinVolume(submitOrderReq.getMinVolume());
		ob.setStopPrice(submitOrderReq.getStopPrice());
		ob.setSequenceNo("1");
		
		ContractField contract = submitOrderReq.getContract();
		double marginRate = submitOrderReq.getDirection() == DirectionEnum.D_Buy ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		// 委托下单会出现三种情况：成功，资金不足，持仓不足
		if (submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
			int vol = submitOrderReq.getVolume();
			double price = submitOrderReq.getPrice();
			double cost = vol * price * contract.getMultiplier() * marginRate + GwConstants.DEFAULT_FEE;
			if(cost > gwAccount.getAccount().getAvailable()) {
				ob.setOrderStatus(OrderStatusEnum.OS_Rejected);
				ob.setStatusMsg("资金不足");
				log.warn("资金不足，无法下单");
				return ob.build();
			}
		} else {
			String unifiedSymbol = contract.getUnifiedSymbol();
			Map<String, PositionField> posMap = submitOrderReq.getDirection() == DirectionEnum.D_Buy 
					? gwPositions.getShortPositionMap() : gwPositions.getLongPositionMap();
			if(!posMap.containsKey(unifiedSymbol)) {
				ob.setOrderStatus(OrderStatusEnum.OS_Rejected);
				ob.setStatusMsg("仓位不足");
				log.warn("仓位不足，无法下单");
				return ob.build();
			}
			int totalAvailable = posMap.get(unifiedSymbol).getPosition() - posMap.get(unifiedSymbol).getFrozen();
			int tdAvailable = posMap.get(unifiedSymbol).getTdPosition() - posMap.get(unifiedSymbol).getTdFrozen();
			int ydAvailable = posMap.get(unifiedSymbol).getYdPosition() - posMap.get(unifiedSymbol).getYdFrozen();
			if(submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_CloseToday && tdAvailable < submitOrderReq.getVolume()
					|| submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_CloseYesterday && ydAvailable < submitOrderReq.getVolume()
					|| totalAvailable < submitOrderReq.getVolume()) {
				ob.setOrderStatus(OrderStatusEnum.OS_Rejected);
				ob.setStatusMsg("仓位不足");
				log.warn("仓位不足，无法下单");
				return ob.build();
			}
		}

		ob.setOrderStatus(OrderStatusEnum.OS_Touched);
		ob.setStatusMsg("报单已提交");
		OrderField of = ob.build();
		orderIdMap.put(of.getOrderId(), of);
		log.info("成功下单：{}", of.toString());
		return of;
	}
	
	public OrderField cancelOrder(CancelOrderReqField cancelOrderReq) {
		boolean hasOrderId = cancelOrderReq.getOrderId() != null && orderIdMap.containsKey(cancelOrderReq.getOrderId());
		if(!hasOrderId) {
			return null;
		}
		
		OrderField order = orderIdMap.remove(cancelOrderReq.getOrderId());
		if(order.getOrderStatus() == OrderStatusEnum.OS_AllTraded) {
			return order;
		}
		
		OrderField.Builder ob = order.toBuilder();
		ob.setOrderStatus(OrderStatusEnum.OS_Canceled);
		ob.setCancelTime(LocalDateTime.now().format(CommonConstant.DT_FORMAT_FORMATTER));
		return ob.build();
	}
	
	public List<TradeField> tryDeal(TickField tick) {
		tradingDay = tick.getTradingDay();
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
					orderIdMap.computeIfPresent(of.getOrderId(), (key, v) -> of);
					
					ContractField contract = contractMap.get(unifiedSymbol);
					// 计算成交
					TradeField trade = TradeField.newBuilder()
							.setTradeId(System.currentTimeMillis()+"")
							.setAccountId(gwAccount.getAccount().getAccountId())
							.setAdapterOrderId("")
							.setContract(contract)
							.setDirection(order.getDirection())
							.setGatewayId(gwAccount.getAccount().getGatewayId())
							.setHedgeFlag(order.getHedgeFlag())
							.setOffsetFlag(order.getOffsetFlag())
							.setOrderId(order.getOrderId())
							.setOriginOrderId(order.getOriginOrderId())
							.setPrice(order.getPrice())
							.setPriceSource(PriceSourceEnum.PSRC_LastPrice)
							.setTradeDate(tradingDay)
							.setTradeTime(LocalTime.now().format(CommonConstant.T_FORMAT_FORMATTER))
							.setVolume(order.getTotalVolume())
							.build();
					
					if(order.getOffsetFlag() == OffsetFlagEnum.OF_Open) {						
						commission += order.getTotalVolume() * GwConstants.DEFAULT_FEE;
					}
					
					tradeList.add(trade);
					tradeOrderMap.put(trade, of);
				}
			}
		});
		
		return tradeList;
	}
	
	public OrderField handleDeal(TradeField trade) {
		OrderField order = tradeOrderMap.remove(trade);
		if(order == null) {
			throw new IllegalArgumentException("不存在对应的挂单");
		}
		return order;
	}
	
	public double gainCommissionThenReset() {
		double val = commission;
		commission = 0D;
		return val;
	}

	public double getTotalFrozenAmount() {
		double totalFrozenAmount = 0;
		Double r1 = orderIdMap.reduce(100, 
			(k, v) -> (v.getTotalVolume() - v.getTradedVolume()) * v.getContract().getMultiplier() * v.getPrice() * v.getContract().getLongMarginRatio(),
			(a, b) -> a + b);
		totalFrozenAmount += r1 == null ? 0 : r1.doubleValue();
		return totalFrozenAmount;
	}
	
	public void proceedDailySettlement() {
		
	}
}
