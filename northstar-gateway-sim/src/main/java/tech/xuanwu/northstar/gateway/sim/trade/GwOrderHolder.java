package tech.xuanwu.northstar.gateway.sim.trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.model.ContractManager;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
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
	 * originOrderId --> TradeIntent
	 */
	private ConcurrentHashMap<String, TradeIntent> originOrderIdMap = new ConcurrentHashMap<>(100);
	/**
	 * unifiedSymbol --> tradingDay
	 */
	private final Map<String,String> tradingDayMap = new HashMap<>(1000);
	
	public GwOrderHolder (String gatewayId, int ticksOfCommission, ContractManager contractMgr) {
		this.gatewayId = gatewayId;
		this.ticksOfCommission = ticksOfCommission;
		this.contractMgr = contractMgr;
	}
	
	protected OrderField tryOrder(SubmitOrderReqField submitOrderReq, AccountField af) {
		if(StringUtils.isEmpty(submitOrderReq.getOriginOrderId())) {
			throw new IllegalArgumentException("originOrderId不能为空");
		}
		ContractField contract = submitOrderReq.getContract();
		double marginRate = submitOrderReq.getDirection() == DirectionEnum.D_Buy ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		int vol = submitOrderReq.getVolume();
		double price = submitOrderReq.getPrice();
		double cost = vol * price * contract.getMultiplier() * marginRate + contract.getPriceTick() * ticksOfCommission;
		TradeIntent tradeIntent = new TradeIntent(submitOrderReq);
		String tradingDay = Optional.ofNullable(tradingDayMap.get(contract.getUnifiedSymbol())).orElse("");
		if(cost > af.getAvailable()) {
			log.warn("资金不足，无法下单。当前可用资金：{}，下单成本：{}，订单：{}", af.getAvailable(), cost, submitOrderReq);
			return tradeIntent.rejectOrder(tradingDay, "资金不足");
		}
		
		OrderField order = tradeIntent.transformOrder(tradingDay);
		originOrderIdMap.put(submitOrderReq.getOriginOrderId(), tradeIntent);
		log.info("成功下单：{}, {}, {}, {}, {}手, {}", submitOrderReq.getOriginOrderId(), submitOrderReq.getContract().getName(), submitOrderReq.getDirection(),
				submitOrderReq.getOffsetFlag(), submitOrderReq.getVolume(), submitOrderReq.getPrice());
		return order;
	}
	
	protected OrderField tryOrder(SubmitOrderReqField submitOrderReq, PositionField pf) {
		if(StringUtils.isEmpty(submitOrderReq.getOriginOrderId())) {
			throw new IllegalArgumentException("originOrderId不能为空");
		}
		TradeIntent tradeIntent = new TradeIntent(submitOrderReq);
		ContractField contract = submitOrderReq.getContract();
		String tradingDay = Optional.ofNullable(tradingDayMap.get(contract.getUnifiedSymbol())).orElse("");
		if(pf == null) {
			log.warn("仓位不足，无法下单：{}, {}, {}, {}, {}手, {}", submitOrderReq.getOriginOrderId(), submitOrderReq.getContract().getName(),
					submitOrderReq.getDirection(), submitOrderReq.getOffsetFlag(), submitOrderReq.getVolume(), submitOrderReq.getPrice());
			return tradeIntent.rejectOrder(tradingDay, "仓位不足");
		}
		int totalAvailable = pf.getPosition() - pf.getFrozen();
		int tdAvailable = pf.getTdPosition() - pf.getTdFrozen();
		int ydAvailable = pf.getYdPosition() - pf.getYdFrozen();
		if(submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_CloseToday && tdAvailable < submitOrderReq.getVolume()
				|| submitOrderReq.getOffsetFlag() == OffsetFlagEnum.OF_CloseYesterday && ydAvailable < submitOrderReq.getVolume()
				|| totalAvailable < submitOrderReq.getVolume()) {
			log.warn("仓位不足，无法下单：{}, {}, {}, {}, {}手, {}", submitOrderReq.getOriginOrderId(), submitOrderReq.getContract().getName(),
					submitOrderReq.getDirection(), submitOrderReq.getOffsetFlag(), submitOrderReq.getVolume(), submitOrderReq.getPrice());
			return tradeIntent.rejectOrder(tradingDay, "仓位不足");
		}
		
		OrderField order = tradeIntent.transformOrder(tradingDay);
		originOrderIdMap.put(submitOrderReq.getOriginOrderId(), tradeIntent);
		log.info("成功下单：{}, {}, {}, {}, {}手, {}", submitOrderReq.getOriginOrderId(), submitOrderReq.getContract().getName(), submitOrderReq.getDirection(),
				submitOrderReq.getOffsetFlag(), submitOrderReq.getVolume(), submitOrderReq.getPrice());
		return order;
	}
	
	protected OrderField cancelOrder(CancelOrderReqField cancelOrderReq) {
		if(StringUtils.isEmpty(cancelOrderReq.getOriginOrderId())) {
			throw new IllegalArgumentException("originOrderId不能为空");
		}
		if(StringUtils.isEmpty(cancelOrderReq.getOrderId()) && StringUtils.isEmpty(cancelOrderReq.getOriginOrderId())) {
			throw new IllegalArgumentException("未提供要撤单的订单号");
		} 
		if(StringUtils.isNotEmpty(cancelOrderReq.getOriginOrderId())) {
			return originOrderIdMap.remove(cancelOrderReq.getOriginOrderId()).cancelOrder();
		}
		return null;
	}
	
	protected List<OrderField> tryDeal(TickField tick) {
		tradingDayMap.put(tick.getUnifiedSymbol(), tick.getTradingDay());
		final String unifiedSymbol = tick.getUnifiedSymbol();
		List<OrderField> tradeList = new ArrayList<>();
		originOrderIdMap.forEach((k, intent) -> {
			if(intent.isContractMatch(unifiedSymbol) && !intent.isTerminated()) {
				// TODO Intent对象什么时候从Map中移除
				if(intent.isTerminated()) {
					return;
				}
				Optional<OrderField> order = intent.tryDeal(tick);
				if(order.isPresent()) {		
					tradeList.add(order.get());
					log.info("模拟成交：{}，{}，{}，{}，{}手，{}，{}", order.get().getOriginOrderId(), order.get().getContract().getName(), 
							order.get().getDirection(), order.get().getOffsetFlag(), order.get().getTradedVolume(), order.get().getPrice(), order.get().getTradingDay());
				}
			}
		});
		
		return tradeList;
	}
	
	protected TradeField transform(OrderField order) {
		if(!originOrderIdMap.containsKey(order.getOriginOrderId())) {
			throw new IllegalArgumentException("找不到订单对应的成交");
		}
		return originOrderIdMap.get(order.getOriginOrderId()).transformTrade();
	}
	
	protected double getFrozenMargin() {
		Double totalFrozenAmount = originOrderIdMap.values().stream()
				.map(intent -> intent.getFrozenMargin())
				.reduce(0D, (a, b) -> a + b);
		return totalFrozenAmount;
	}
	
	protected void proceedDailySettlement() {
		
	}
}
