package tech.xuanwu.northstar.gateway.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.protobuf.InvalidProtocolBufferException;

import tech.xuanwu.northstar.common.exception.TradeException;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

class GwPositionHolder {

	private String gatewayId;
	
	private Map<String, ContractField> contractMap;
	
	/**
	 * unifiedSymbol -> positionBuilder
	 */
	private ConcurrentHashMap<String, PositionField.Builder> longPositionMap = new ConcurrentHashMap<>(100);
	private ConcurrentHashMap<String, PositionField.Builder> shortPositionMap = new ConcurrentHashMap<>(100);
	
	/**
	 * orderId -> positionBuilder
	 */
	private Map<String, PositionField.Builder> frozenPositionMap = new HashMap<>();
	
	private Set<OrderStatusEnum> nonTradeStates = new HashSet<>() {
		private static final long serialVersionUID = 1L;

		{
			add(OrderStatusEnum.OS_NoTradeNotQueueing);
			add(OrderStatusEnum.OS_NoTradeQueueing);
			add(OrderStatusEnum.OS_NotTouched);
			add(OrderStatusEnum.OS_Touched);
		}
	};
	
	private Set<OrderStatusEnum> partialTradeStates = new HashSet<>() {
		private static final long serialVersionUID = 1L;

		{
			add(OrderStatusEnum.OS_PartTradedNotQueueing);
			add(OrderStatusEnum.OS_PartTradedQueueing);
		}
	};
	
	private Set<OrderStatusEnum> validStatesToUnfrozen = new HashSet<>() {
		private static final long serialVersionUID = 1L;

		{
			add(OrderStatusEnum.OS_AllTraded);
			add(OrderStatusEnum.OS_Canceled);
		}
	};
	
	protected GwPositionHolder(String gatewayId, Map<String, ContractField> contractMap) {
		this.gatewayId = gatewayId;
		this.contractMap = contractMap;
	}
	
	/**
	 * 行情更新
	 * @param tick
	 */
	protected List<Optional<PositionField>> updatePositionBy(TickField tick) {
		return List.of(
				updatePositionBy(tick, longPositionMap), 
				updatePositionBy(tick, shortPositionMap));
	}
	
	/**
	 * 更新持仓
	 * @param trade
	 * @return			平仓盈亏
	 */
	protected double updatePositionBy(TradeField trade) {
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Unkonwn) {
			throw new TradeException("未定义开平仓类型");
		}
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
			addPosition(trade);
			return 0D;
		} 
		PositionField pf = reducePosition(trade);
		double priceDiff = (trade.getPrice() - pf.getPrice()) * (pf.getPositionDirection() == PositionDirectionEnum.PD_Long ? 1 : -1);
		return priceDiff * pf.getContract().getMultiplier() * trade.getVolume();
	}
	
	/**
	 * 更新持仓
	 * @param order
	 */
	protected PositionField updatePositionBy(OrderField order) {
		if(order.getOffsetFlag() == OffsetFlagEnum.OF_Unkonwn) {
			throw new TradeException("未定义开平仓操作");
		}
		// 开仓订单对应的持仓更新
		if(order.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
			return null;
		}
		
		// 平仓订单对应的持仓更新
		if(nonTradeStates.contains(order.getOrderStatus())) {
			return frozenPosition(order);
		} else if(partialTradeStates.contains(order.getOrderStatus())) {
			return refrozenPosition(order);
		} else if(validStatesToUnfrozen.contains(order.getOrderStatus())) {
			return unfrozenPosition(order);
		}
		
		String unifiedSymbol = order.getContract().getUnifiedSymbol();
		Map<String, PositionField.Builder> posMap = getPositionMapBy(order);
		return posMap.get(unifiedSymbol).build();
	}
	
	/**
	 * 获取目标仓位
	 * @param req
	 * @return
	 */
	protected PositionField getPositionByReq(SubmitOrderReqField req) {
		if(req.getDirection() == DirectionEnum.D_Unknown) {
			throw new TradeException("未定义买卖方向");
		}
		String unifiedSymbol = req.getContract().getUnifiedSymbol();
		PositionField.Builder pf = null;
		if(req.getDirection() == DirectionEnum.D_Buy) {
			pf = shortPositionMap.get(unifiedSymbol);
		}else {
			pf = longPositionMap.get(unifiedSymbol);
		}
		if(pf == null) {
			return null;
		}
		return pf.build();
	}
	
	/**
	 * 获取所有持仓
	 * @return
	 */
	protected List<PositionField> getPositions(){
		List<PositionField> posList = new ArrayList<>(longPositionMap.size() + shortPositionMap.size());
		posList.addAll(longPositionMap.values().stream().map(pb -> pb.build()).collect(Collectors.toList()));
		posList.addAll(shortPositionMap.values().stream().map(pb -> pb.build()).collect(Collectors.toList()));
		return posList;
	}
	
	/**
	 * 恢复持仓数据
	 * @param dataList
	 * @throws InvalidProtocolBufferException 
	 */
	protected void restore(List<byte[]> dataList) throws InvalidProtocolBufferException {
		for(byte[] data : dataList) {
			PositionField.Builder pb = PositionField.newBuilder().mergeFrom(data);
			if(pb.getPositionDirection() == PositionDirectionEnum.PD_Long) {
				longPositionMap.put(pb.getContract().getUnifiedSymbol(), pb);
			} else {
				shortPositionMap.put(pb.getContract().getUnifiedSymbol(), pb);
			}
		}
	}
	
	/**
	 * 开仓或加仓
	 * @param tradeField
	 */
	private PositionField addPosition(TradeField tradeField) {
		ContractField contract = tradeField.getContract();
		Map<String, PositionField.Builder> posMap = getPositionMapBy(tradeField);
		PositionField.Builder pb = posMap.get(contract.getUnifiedSymbol());
		double marginRatio = tradeField.getDirection() == DirectionEnum.D_Buy ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		String unifiedSymbol = contract.getUnifiedSymbol();
		HedgeFlagEnum hedgeFlag = tradeField.getHedgeFlag();
		PositionDirectionEnum direction = tradeField.getDirection() == DirectionEnum.D_Buy ? PositionDirectionEnum.PD_Long : PositionDirectionEnum.PD_Short;
		String accountId = gatewayId;
		String positionId = unifiedSymbol + "@" + direction.getValueDescriptor().getName() + "@" + hedgeFlag.getValueDescriptor().getName() + "@" + accountId;
		if(pb == null) {
			// 开新仓逻辑
			final PositionField.Builder npb = PositionField.newBuilder()
					.setAccountId(gatewayId)
					.setGatewayId(gatewayId)
					.setContract(contract)
					.setContractValue(tradeField.getVolume() * tradeField.getPrice() * contract.getMultiplier())
					.setExchangeMargin(tradeField.getVolume() * tradeField.getPrice() * contract.getMultiplier() * marginRatio)
					.setUseMargin(tradeField.getVolume() * tradeField.getPrice() * contract.getMultiplier() * marginRatio)
					.setHedgeFlag(tradeField.getHedgeFlag())
					.setLastPrice(tradeField.getPrice())
					.setOpenPrice(tradeField.getPrice())
					.setPrice(tradeField.getPrice())
					.setPosition(tradeField.getVolume())
					.setPositionDirection(direction)
					.setPositionId(positionId)
					.setTdPosition(tradeField.getVolume());
			posMap.compute(unifiedSymbol, (k, v) -> npb);
			return npb.build();
		}
		
		// 加仓逻辑
		int nPosition = tradeField.getVolume();
		int oPosition = pb.getPosition();
		int tdPosition = pb.getTdPosition();

		// 计算成本
		double cost = pb.getPrice() * pb.getPosition() * contract.getMultiplier();
		double nCost = tradeField.getPrice() * tradeField.getVolume() * contract.getMultiplier();
		double openCost = pb.getOpenPrice() * pb.getPosition() * contract.getMultiplier();
		
		pb.setTdPosition(tdPosition + nPosition)
			.setPosition(oPosition + nPosition)
			.setPrice((cost + nCost) / ((oPosition + nPosition) * contract.getMultiplier()))
			.setOpenPrice((openCost + nCost) / ((oPosition + nPosition) * contract.getMultiplier()))
			.setContractValue(openCost + nCost)
			.setUseMargin((openCost + nCost) * marginRatio)
			.setExchangeMargin((openCost + nCost) * marginRatio)
			.build();
		return pb.build();
	}
	
	/**
	 * 减仓
	 * @param tradeField
	 */
	private PositionField reducePosition(TradeField tradeField) {
		ContractField contract = tradeField.getContract();
		Map<String, PositionField.Builder> posMap = getPositionMapBy(tradeField);
		double marginRatio = tradeField.getDirection() == DirectionEnum.D_Buy ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		PositionField.Builder pb = posMap.get(contract.getUnifiedSymbol());
		if(pb == null) {
			throw new IllegalStateException("没有相应的持仓对应成交：" + tradeField.toString());
		}
		
		int vol = tradeField.getVolume();

		if(tradeField.getOffsetFlag()==OffsetFlagEnum.OF_CloseToday) {
			pb.setTdPosition(pb.getTdPosition() - vol);	
			pb.setPosition(pb.getPosition() - vol);
		}else if(tradeField.getOffsetFlag()==OffsetFlagEnum.OF_CloseYesterday) {
			pb.setYdPosition(pb.getYdPosition() - vol);
			pb.setPosition(pb.getPosition() - vol);
		}else {
			pb.setPosition(pb.getPosition() - vol);
			pb.setYdPosition(pb.getYdPosition() - Math.max(0, (vol - pb.getTdPosition())));
			pb.setTdPosition(pb.getTdPosition() - Math.min(pb.getTdPosition(), vol));
		}
		pb.setExchangeMargin(pb.getOpenPrice() * pb.getPosition() * contract.getMultiplier() * marginRatio);
		pb.setUseMargin(pb.getExchangeMargin());
		
		return pb.build();
		
	}
	
	/**
	 * 冻结仓位
	 * 只有有效的平仓委托才会冻结持仓仓位
	 * @param order
	 * @return
	 */
	private PositionField frozenPosition(OrderField order) {
		ContractField contract = order.getContract();
		Map<String, PositionField.Builder> posMap = getPositionMapBy(order);
		PositionField.Builder pb = posMap.get(contract.getUnifiedSymbol());
		PositionField.Builder frozenPosition = PositionField.newBuilder();
		frozenPosition.setPositionId(pb.getPositionId());
		frozenPosition.setContract(contract);
		frozenPosition.setPositionDirection(pb.getPositionDirection());
		if(order.getOffsetFlag() == OffsetFlagEnum.OF_CloseToday) {
			frozenPosition.setTdFrozen(order.getTotalVolume());
			frozenPosition.setFrozen(order.getTotalVolume());
		}else if(order.getOffsetFlag() == OffsetFlagEnum.OF_CloseYesterday) {
			frozenPosition.setYdFrozen(order.getTotalVolume());
			frozenPosition.setFrozen(order.getTotalVolume());
		}else {
			frozenPosition.setFrozen(order.getTotalVolume());
			int tdFrozen = Math.min(order.getTotalVolume(), pb.getTdPosition());
			int ydFrozen = order.getTotalVolume() - tdFrozen;
			frozenPosition.setYdFrozen(ydFrozen);
			frozenPosition.setTdFrozen(tdFrozen);
		}
		
		frozenPositionMap.put(order.getOrderId(), frozenPosition);
		pb = updateFrozen(pb, frozenPosition, 1);
		return pb.build();
	}
	
	private PositionField refrozenPosition(OrderField order) {
		ContractField contract = order.getContract();
		Map<String, PositionField.Builder> posMap = getPositionMapBy(order);
		PositionField.Builder pb = posMap.get(contract.getUnifiedSymbol());
		PositionField.Builder frozenPosition = frozenPositionMap.get(order.getOrderId());
		if(order.getOffsetFlag() == OffsetFlagEnum.OF_CloseToday) {
			frozenPosition.setTdFrozen(order.getTotalVolume() - order.getTradedVolume());
			frozenPosition.setFrozen(order.getTotalVolume() - order.getTradedVolume());
		}else if(order.getOffsetFlag() == OffsetFlagEnum.OF_CloseYesterday) {
			frozenPosition.setYdFrozen(order.getTotalVolume() - order.getTradedVolume());
			frozenPosition.setFrozen(order.getTotalVolume() - order.getTradedVolume());
		}else {
			frozenPosition.setFrozen(order.getTotalVolume() - order.getTradedVolume());
			int tdFrozen = Math.min(order.getTotalVolume() - order.getTradedVolume(), pb.getTdPosition());
			int ydFrozen = order.getTotalVolume() - tdFrozen;
			frozenPosition.setYdFrozen(ydFrozen);
			frozenPosition.setTdFrozen(tdFrozen);
		}
		
		pb = updateFrozen(pb, frozenPosition, 1);
		return pb.build();
	}
	
	/**
	 * 解冻仓位
	 * 只有当订单撤单和全部成交才需要进行解冻
	 * @param order
	 * @return
	 */
	private PositionField unfrozenPosition(OrderField order) {
		ContractField contract = order.getContract();
		
		Map<String, PositionField.Builder> posMap = getPositionMapBy(order);
		PositionField.Builder pb = posMap.get(contract.getUnifiedSymbol());
		if(pb == null) {
			throw new IllegalStateException("持仓状态与期望不一致");
		}
		if(order.getOrderStatus() == OrderStatusEnum.OS_AllTraded || order.getOrderStatus() == OrderStatusEnum.OS_Canceled) {
			// 撤单或全部成交时
			PositionField.Builder frozenPos = frozenPositionMap.remove(order.getOrderId());			
			pb = updateFrozen(pb, frozenPos, -1);
		}
		
		return pb.build();
	}
	
	private PositionField.Builder updateFrozen(PositionField.Builder srcPos, PositionField.Builder frozenPos, int addOrMinus) {
		srcPos.setFrozen(srcPos.getFrozen() + addOrMinus * frozenPos.getFrozen());
		srcPos.setTdFrozen(srcPos.getTdFrozen() + addOrMinus * frozenPos.getTdFrozen());
		srcPos.setYdFrozen(srcPos.getYdFrozen() + addOrMinus * frozenPos.getYdFrozen());
		return srcPos;
	}
	
	private Optional<PositionField> updatePositionBy(TickField tick, ConcurrentHashMap<String, PositionField.Builder> positionMap) {
		ContractField contract = contractMap.get(tick.getUnifiedSymbol());
		if(contract == null) {
			return Optional.empty();
		}
		PositionField.Builder sp = positionMap.get(contract.getUnifiedSymbol());
		if(sp != null) {
			PositionField.Builder slp = updatePosition(sp, contract, tick);
			positionMap.compute(contract.getUnifiedSymbol(), (k, v) -> slp);
			return Optional.of(slp.build());
		}
		return Optional.empty();
	}
	
	private PositionField.Builder updatePosition(PositionField.Builder pb, ContractField contract, TickField tick) {
		pb.setLastPrice(tick.getLastPrice());
		// TODO 准确化持仓占用保证金额度的计算
//		double marginRatio = pb.getPositionDirection() == PositionDirectionEnum.PD_Long ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
//		double margin = pb.getOpenPrice() * pb.getPosition() * contract.getMultiplier() * marginRatio;
//		pb.setExchangeMargin(margin);
//		pb.setUseMargin(margin);
		pb.setPriceDiff(pb.getPositionDirection()==PositionDirectionEnum.PD_Long ? (tick.getLastPrice() - pb.getPrice()) : (pb.getPrice() - tick.getLastPrice()));
		pb.setOpenPriceDiff(pb.getPositionDirection()==PositionDirectionEnum.PD_Long ? (tick.getLastPrice() - pb.getOpenPrice()) : (pb.getOpenPrice() - tick.getLastPrice()));
		pb.setPositionProfit(pb.getPosition() * pb.getPriceDiff() * contract.getMultiplier());
		pb.setPositionProfitRatio(pb.getUseMargin() == 0  ? 0 : pb.getPositionProfit() / pb.getUseMargin());
		pb.setOpenPositionProfit(pb.getPosition() * pb.getOpenPriceDiff() * contract.getMultiplier());
		pb.setOpenPositionProfitRatio(pb.getUseMargin() == 0  ? 0 : pb.getOpenPositionProfit() / pb.getUseMargin());
		pb.setContractValue(tick.getLastPrice() * pb.getPosition() * contract.getMultiplier());
		
		return pb;
	}
	
	/**
	 * 总持仓盈亏
	 * @return
	 */
	protected double getTotalPositionProfit() {
		double totalPositionProfit = 0;
		Double r1 = longPositionMap.reduce(100,  (k, v) -> v.getPositionProfit(), (v1, v2) -> v1 + v2);
		Double r2 = shortPositionMap.reduce(100,  (k, v) -> v.getPositionProfit(), (v1, v2) -> v1 + v2);
		totalPositionProfit += r1 == null ? 0 : r1.doubleValue();
		totalPositionProfit += r2 == null ? 0 : r2.doubleValue();
		return totalPositionProfit;
	}
	
	/**
	 * 总占用保证金
	 * @return
	 */
	protected double getTotalUseMargin() {
		double totalUseMargin = 0;
		Double r1 = longPositionMap.reduce(100,  (k, v) -> v.getUseMargin(), (v1, v2) -> v1 + v2);
		Double r2 = shortPositionMap.reduce(100,  (k, v) -> v.getUseMargin(), (v1, v2) -> v1 + v2);
		totalUseMargin += r1 == null ? 0 : r1.doubleValue();
		totalUseMargin += r2 == null ? 0 : r2.doubleValue();
		return totalUseMargin;
	}
	
	private Map<String, PositionField.Builder> getPositionMapBy(TradeField trade) {
		DirectionEnum direction = trade.getDirection();
		OffsetFlagEnum kpType = trade.getOffsetFlag();
		if(kpType == OffsetFlagEnum.OF_Unkonwn) {
			throw new IllegalStateException("未知开平仓类型");
		}
		Map<String, PositionField.Builder> posMap = 
				direction == DirectionEnum.D_Buy && kpType == OffsetFlagEnum.OF_Open || direction == DirectionEnum.D_Sell && kpType != OffsetFlagEnum.OF_Open
				? longPositionMap : shortPositionMap;
		return posMap;
	}
	
	private Map<String, PositionField.Builder> getPositionMapBy(OrderField order) {
		DirectionEnum direction = order.getDirection();
		OffsetFlagEnum kpType = order.getOffsetFlag(); 
		if(kpType == OffsetFlagEnum.OF_Unkonwn) {
			throw new IllegalStateException("未知开平仓类型");
		}
		Map<String, PositionField.Builder> posMap = 
				direction == DirectionEnum.D_Buy && kpType == OffsetFlagEnum.OF_Open || direction == DirectionEnum.D_Sell && kpType != OffsetFlagEnum.OF_Open
				? longPositionMap : shortPositionMap;
		return posMap;
	}
	
}
