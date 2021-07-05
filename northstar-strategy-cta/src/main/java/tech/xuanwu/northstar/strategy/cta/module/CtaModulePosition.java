package tech.xuanwu.northstar.strategy.cta.module;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

import tech.xuanwu.northstar.strategy.common.ModulePosition;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 用于记录模组持仓状态,以计算持仓盈亏,持仓状态,持仓时间,以及冻结仓位情况
 * @author KevinHuangwl
 *
 */
public class CtaModulePosition implements ModulePosition{
	
	/**
	 * 暂存开仓成交,用于计算持仓盈亏,以及计算持仓状态
	 * unifiedSymbol --> trade
	 */
	private Map<String, LinkedList<TradeField>> openTradeMap = new HashMap<>();
	
	private LocalDateTime lastOpeningTime;
	
	/**
	 * TradeField --> Profit
	 */
	private Map<TradeField, AtomicInteger> positionProfitMap = new HashMap<>();
	
	public CtaModulePosition() {}
	
	public CtaModulePosition(List<TradeField> openTrades) {
		for(TradeField t : openTrades) {
			if(t.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
				String unifiedSymbol = t.getContract().getUnifiedSymbol();
				this.openTradeMap.putIfAbsent(unifiedSymbol, new LinkedList<>());
				this.openTradeMap.get(unifiedSymbol).add(t);
				this.positionProfitMap.put(t, new AtomicInteger(0));
				if(lastOpeningTime == null) {
					lastOpeningTime = LocalDateTime.ofEpochSecond(t.getTradeTimestamp() / 1000, 0, ZoneOffset.ofHours(8));
				}
			}
		}
	}

	@Override
	public List<TradeField> getOpenningTrade() {
		List<TradeField> result = new ArrayList<>();
		for(Entry<String, LinkedList<TradeField>> e : openTradeMap.entrySet()) {
			result.addAll(e.getValue());
		}
		return result;
	}

	@Override
	public Duration getPositionDuration() {
		if(lastOpeningTime != null) {
			return Duration.between(lastOpeningTime, LocalDateTime.now());
		}
		return Duration.ZERO;
	}

	@Override
	public void onTick(TickField tick) {
		for(Entry<TradeField, AtomicInteger> e : positionProfitMap.entrySet()) {
			TradeField trade = e.getKey();
			if(StringUtils.equals(tick.getUnifiedSymbol(), trade.getContract().getUnifiedSymbol())) {
				int factor = trade.getDirection() == DirectionEnum.D_Buy ? 1 : -1;
				double priceDif = factor * (tick.getLastPrice() - trade.getPrice());
				e.getValue().set((int) (priceDif * trade.getVolume() * trade.getContract().getMultiplier()));
			}
		}
	}

	// TODO 以后再考虑锁仓情况
	@Override
	public void onTrade(TradeField trade) {
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Unkonwn) {
			throw new IllegalStateException("未定义成交类型");
		}
		String unifiedSymbol = trade.getContract().getUnifiedSymbol();
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
			//开仓成交
			openTradeMap.putIfAbsent(unifiedSymbol, new LinkedList<>());
			openTradeMap.get(unifiedSymbol).add(trade);
			positionProfitMap.putIfAbsent(trade, new AtomicInteger(0));
			if(lastOpeningTime == null) {
				lastOpeningTime = LocalDateTime.ofEpochSecond(trade.getTradeTimestamp() / 1000, 0, ZoneOffset.ofHours(8));
			}
		}else {
			//平仓成交
			int targetConsume = trade.getVolume();
			while(targetConsume > 0) {
				TradeField openingDeal = openTradeMap.get(unifiedSymbol).peekFirst();
				positionProfitMap.remove(openingDeal);
				if(openingDeal.getVolume() < targetConsume) {
					targetConsume -= openingDeal.getVolume();
				} else if(openingDeal.getVolume() > targetConsume) {
					int volDif = openingDeal.getVolume() - targetConsume;
					targetConsume = 0;
					TradeField restTrade = TradeField.newBuilder(openingDeal).setVolume(volDif).build();
					openTradeMap.get(unifiedSymbol).offerFirst(restTrade);
				} else {					
					lastOpeningTime = null;
				}
			}
		}
	}

	@Override
	public int getPositionProfit() {
		return positionProfitMap.values()
				.stream()
				.reduce(0, (i, item) -> i + item.get(), (a, b) -> a + b);
	}
}
