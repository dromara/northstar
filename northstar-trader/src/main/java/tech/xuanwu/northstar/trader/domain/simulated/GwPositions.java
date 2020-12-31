package tech.xuanwu.northstar.trader.domain.simulated;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 模拟盘持仓计算
 * 同步锁设计为轻量级锁，因为模拟网关理论上只会不断接收tick事件线程的更新，偶尔会接收到挂单与撤单的接口线程调用
 * @author kevinhuangwl
 *
 */
public class GwPositions {
	
	private ConcurrentHashMap<String, PositionField> longPositionMap = new ConcurrentHashMap<>(100);
	private ConcurrentHashMap<String, PositionField> shortPositionMap = new ConcurrentHashMap<>(100);
	
	private volatile double totalCloseProfit;
	private GwAccount gwAccount;
	private Map<String, ContractField> contractMap;
	private Map<String, PositionField> frozenPositionMap = new HashMap<>();
	
	public GwPositions() {}
	
	public GwPositions(ConcurrentHashMap<String, PositionField> longPositionMap, ConcurrentHashMap<String, PositionField> shortPositionMap) {
		this.longPositionMap = longPositionMap;
		this.shortPositionMap = shortPositionMap;
	}
	
	public void setGwAccount(GwAccount gwAccount) {
		this.gwAccount = gwAccount;
	}
	
	public void setContractMap(Map<String, ContractField> contractMap) {
		this.contractMap = contractMap;
	}
	
	/**
	 * 开仓或加仓
	 * @param tradeField
	 */
	public synchronized PositionField addPosition(TradeField tradeField) {
		ContractField contract = tradeField.getContract();
		Map<String, PositionField> posMap = getPositionMapBy(tradeField);
		PositionField p = posMap.get(contract.getUnifiedSymbol());
		double marginRatio = tradeField.getDirection() == DirectionEnum.D_Buy ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		String uniqueSymbol = contract.getUnifiedSymbol();
		HedgeFlagEnum hedgeFlag = tradeField.getHedgeFlag();
		PositionDirectionEnum direction = tradeField.getDirection() == DirectionEnum.D_Buy ? PositionDirectionEnum.PD_Long : PositionDirectionEnum.PD_Short;
		String accountId = gwAccount.getAccount().getAccountId();
		String positionId = uniqueSymbol + "@" + direction.getValueDescriptor().getName() + "@" + hedgeFlag.getValueDescriptor().getName() + "@" + accountId;
		if(p == null) {
			// 开新仓逻辑
			final PositionField np = PositionField.newBuilder()
					.setAccountId(gwAccount.getAccount().getAccountId())
					.setGatewayId(gwAccount.getAccount().getGatewayId())
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
					.setTdPosition(tradeField.getVolume())
					.build();
			posMap.compute(contract.getUnifiedSymbol(), (k, v) -> np);
			return np;
		}
		
		// 加仓逻辑
		PositionField.Builder npb = p.toBuilder();
		int nPosition = tradeField.getVolume();
		int oPosition = p.getPosition();
		int tdPosition = p.getTdPosition();

		// 计算成本
		double cost = p.getPrice() * p.getPosition() * contract.getMultiplier();
		double nCost = tradeField.getPrice() * tradeField.getVolume() * contract.getMultiplier();
		double openCost = p.getOpenPrice() * p.getPosition() * contract.getMultiplier();
		
		final PositionField np = npb
			.setTdPosition(tdPosition + nPosition)
			.setPosition(oPosition + nPosition)
			.setPrice((cost + nCost) / ((oPosition + nPosition) * contract.getMultiplier()))
			.setOpenPrice((openCost + nCost) / ((oPosition + nPosition) * contract.getMultiplier()))
			.setContractValue(openCost + nCost)
			.setUseMargin((openCost + nCost) * marginRatio)
			.setExchangeMargin((openCost + nCost) * marginRatio)
			.build();
		posMap.compute(contract.getUnifiedSymbol(), (k, v) -> np);
		return np;
	}
	
	/**
	 * 减仓
	 * @param tradeField
	 */
	public synchronized PositionField reducePosition(TradeField tradeField) {
		ContractField contract = tradeField.getContract();
		Map<String, PositionField> posMap = getPositionMapBy(tradeField);
		double marginRatio = tradeField.getDirection() == DirectionEnum.D_Buy ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		PositionField p = posMap.get(contract.getUnifiedSymbol());
		if(p == null) {
			throw new IllegalStateException("没有相应的持仓对应成交：" + tradeField.toString());
		}
		
		PositionField.Builder npb = p.toBuilder();
		
		int vol = tradeField.getVolume();

		if(tradeField.getOffsetFlag()==OffsetFlagEnum.OF_CloseToday) {
			npb.setTdPosition(p.getTdPosition() - vol);	
			npb.setPosition(p.getPosition() - vol);
		}else if(tradeField.getOffsetFlag()==OffsetFlagEnum.OF_CloseYesterday) {
			npb.setYdPosition(p.getYdPosition() - vol);
			npb.setPosition(p.getPosition() - vol);
		}else {
			npb.setPosition(p.getPosition() - vol);
			npb.setYdPosition(p.getYdPosition() - Math.max(0, (vol - p.getTdPosition())));
			npb.setTdPosition(p.getTdPosition() - Math.min(p.getTdPosition(), vol));
		}
		npb.setExchangeMargin(p.getOpenPrice() * npb.getPosition() * contract.getMultiplier() * marginRatio);
		npb.setUseMargin(npb.getExchangeMargin());
		
		int priceDir = p.getPositionDirection() == PositionDirectionEnum.PD_Long ? 1 : -1;
		totalCloseProfit += priceDir * (tradeField.getPrice() - p.getOpenPrice()) * vol * contract.getMultiplier();
		
		PositionField np = npb.build();
		posMap.compute(contract.getUnifiedSymbol(), (k, v) -> np);
		return np;
		
	}
	
	/**
	 * 冻结仓位
	 * @param order
	 * @return
	 */
	public synchronized PositionField frozenPosition(OrderField order) {
		if(order.getOrderStatus() == OrderStatusEnum.OS_AllTraded || order.getOrderStatus() == OrderStatusEnum.OS_Canceled
				|| order.getOrderStatus() == OrderStatusEnum.OS_Unknown || order.getOrderStatus() == OrderStatusEnum.OS_Rejected
				|| order.getOffsetFlag() == OffsetFlagEnum.OF_Open || order.getOffsetFlag() == OffsetFlagEnum.OF_Unkonwn) {
			throw new IllegalArgumentException("订单状态不正确");
		}
		ContractField contract = order.getContract();
		Map<String, PositionField> posMap = getPositionMapBy(order);
		PositionField p = posMap.get(contract.getUnifiedSymbol());
		PositionField.Builder npb = p.toBuilder();
		if(order.getOffsetFlag() == OffsetFlagEnum.OF_CloseToday) {
			npb.setTdFrozen(p.getTdFrozen() + order.getTotalVolume());
			npb.setFrozen(p.getFrozen() + order.getTotalVolume());
		}else if(order.getOffsetFlag() == OffsetFlagEnum.OF_CloseYesterday) {
			npb.setYdFrozen(p.getYdFrozen() + order.getTotalVolume());
			npb.setFrozen(p.getFrozen() + order.getTotalVolume());
		}else {
			npb.setFrozen(p.getFrozen() + order.getTotalVolume());
			int tdFrozen = Math.min(order.getTotalVolume(), npb.getTdPosition());
			int ydFrozen = order.getTotalVolume() + npb.getTdFrozen() - tdFrozen + npb.getYdFrozen();
			npb.setYdFrozen(ydFrozen);
			npb.setTdFrozen(tdFrozen);
		}
		
		PositionField.Builder frozenPosition = PositionField.newBuilder();
		frozenPosition.setFrozen(npb.getFrozen());
		frozenPosition.setTdFrozen(npb.getTdFrozen());
		frozenPosition.setYdFrozen(npb.getYdFrozen());
		frozenPositionMap.put(order.getOrderId(), frozenPosition.build());
		PositionField np = npb.build();
		posMap.compute(contract.getUnifiedSymbol(), (k, v) -> np);
		return np;
	}
	
	/**
	 * 解冻仓位
	 * @param order
	 * @return
	 */
	public synchronized PositionField unfrozenPosition(OrderField order) {
		if(order.getOrderStatus() != OrderStatusEnum.OS_AllTraded && order.getOrderStatus() != OrderStatusEnum.OS_Canceled
				|| order.getOrderStatus() == OrderStatusEnum.OS_Unknown || order.getOrderStatus() == OrderStatusEnum.OS_Rejected) {
			throw new IllegalArgumentException("订单状态不正确");
		}
		ContractField contract = order.getContract();
		Map<String, PositionField> posMap = getPositionMapBy(order);
		PositionField p = posMap.get(contract.getUnifiedSymbol());
		if(p == null) {
			throw new IllegalStateException("持仓状态与期望不一致");
		}
		PositionField.Builder npb = p.toBuilder();
		PositionField frozenPosition = frozenPositionMap.remove(order.getOrderId());
		
		npb.setFrozen(npb.getFrozen() - frozenPosition.getFrozen());
		npb.setYdFrozen(npb.getYdFrozen() - frozenPosition.getYdFrozen());
		npb.setTdFrozen(npb.getTdFrozen() - frozenPosition.getTdFrozen());
		
		PositionField np = npb.build();
		posMap.compute(contract.getUnifiedSymbol(), (k, v) -> np);
		return np;
	}
	
	/**
	 * 行情更新
	 * @param tick
	 */
	public synchronized PositionField updateLongPositionBy(TickField tick) {
		return updatePositionBy(tick, longPositionMap);
	}
	
	/**
	 * 行情更新
	 * @param tick
	 */
	public synchronized PositionField updateShortPositionBy(TickField tick) {
		return updatePositionBy(tick, shortPositionMap);
	}
	
	private PositionField updatePositionBy(TickField tick, ConcurrentHashMap<String, PositionField> positionMap) {
		ContractField contract = contractMap.get(tick.getUnifiedSymbol());
		PositionField sp = positionMap.get(contract.getUnifiedSymbol());
		if(sp != null) {
			PositionField slp = updatePosition(sp, contract, tick);
			positionMap.compute(contract.getUnifiedSymbol(), (k, v) -> slp);
			return slp;
		}
		return null;
	}
	
	private PositionField updatePosition(PositionField p, ContractField contract, TickField tick) {
		PositionField.Builder npb = p.toBuilder();
		npb.setLastPrice(tick.getLastPrice());
		npb.setPriceDiff(p.getPositionDirection()==PositionDirectionEnum.PD_Long ? (tick.getLastPrice() - p.getPrice()) : (p.getPrice() - tick.getLastPrice()));
		npb.setOpenPriceDiff(p.getPositionDirection()==PositionDirectionEnum.PD_Long ? (tick.getLastPrice() - p.getOpenPrice()) : (p.getOpenPrice() - tick.getLastPrice()));
		npb.setPositionProfit(p.getPosition() * npb.getPriceDiff() * contract.getMultiplier());
		npb.setPositionProfitRatio(npb.getUseMargin() == 0  ? 0 : npb.getPositionProfit() / npb.getUseMargin());
		npb.setOpenPositionProfit(npb.getPosition() * npb.getOpenPriceDiff() * contract.getMultiplier());
		npb.setOpenPositionProfitRatio(npb.getUseMargin() == 0  ? 0 : npb.getOpenPositionProfit() / npb.getUseMargin());
		npb.setContractValue(tick.getLastPrice() * npb.getPosition() * contract.getMultiplier());
		
		return npb.build();
	}
	
	/**
	 * 结转当天总平仓盈亏
	 * @return
	 */
	public double gainCloseProfitThenReset() {
		double val = totalCloseProfit;
		totalCloseProfit = 0D;
		return val;
	}
	
	
	/**
	 * 总持仓盈亏
	 * @return
	 */
	public double getTotalPositionProfit() {
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
	public double getTotalUseMargin() {
		double totalUseMargin = 0;
		Double r1 = longPositionMap.reduce(100,  (k, v) -> v.getUseMargin(), (v1, v2) -> v1 + v2);
		Double r2 = shortPositionMap.reduce(100,  (k, v) -> v.getUseMargin(), (v1, v2) -> v1 + v2);
		totalUseMargin += r1 == null ? 0 : r1.doubleValue();
		totalUseMargin += r2 == null ? 0 : r2.doubleValue();
		return totalUseMargin;
	}
	
	public ConcurrentHashMap<String, PositionField> getLongPositionMap(){
		ConcurrentHashMap<String, PositionField> resultMap = new ConcurrentHashMap<>(longPositionMap.size());
		resultMap.putAll(longPositionMap);
		return resultMap;
	}
	
	public ConcurrentHashMap<String, PositionField> getShortPositionMap(){
		ConcurrentHashMap<String, PositionField> resultMap = new ConcurrentHashMap<>(shortPositionMap.size());
		resultMap.putAll(shortPositionMap);
		return resultMap;
	}
	
	/**
	 * 日结算
	 */
	public synchronized void proceedDailySettlement(TickField tick) {
	}
	
	
	private Map<String, PositionField> getPositionMapBy(TradeField trade) {
		DirectionEnum direction = trade.getDirection();
		OffsetFlagEnum kpType = trade.getOffsetFlag();
		if(kpType == OffsetFlagEnum.OF_Unkonwn) {
			throw new IllegalStateException("未知开平仓类型");
		}
		Map<String, PositionField> posMap = 
				direction == DirectionEnum.D_Buy && kpType == OffsetFlagEnum.OF_Open || direction == DirectionEnum.D_Sell && kpType != OffsetFlagEnum.OF_Open
				? longPositionMap : shortPositionMap;
		return posMap;
	}
	
	private Map<String, PositionField> getPositionMapBy(OrderField order) {
		DirectionEnum direction = order.getDirection();
		OffsetFlagEnum kpType = order.getOffsetFlag(); 
		if(kpType == OffsetFlagEnum.OF_Unkonwn) {
			throw new IllegalStateException("未知开平仓类型");
		}
		Map<String, PositionField> posMap = 
				direction == DirectionEnum.D_Buy && kpType == OffsetFlagEnum.OF_Open || direction == DirectionEnum.D_Sell && kpType != OffsetFlagEnum.OF_Open
				? longPositionMap : shortPositionMap;
		return posMap;
	}
}
