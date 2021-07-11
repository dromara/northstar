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

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.ModulePosition;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 用于记录模组持仓状态,以计算持仓盈亏,持仓状态,持仓时间,以及冻结仓位情况
 * 注意：对于CTA策略而言，其持仓在任意时间只应该有且仅有一个合约品种持仓
 * 若多于一个合约品种，属于异常情况
 * @author KevinHuangwl
 *
 */
@Slf4j
public class CtaModulePosition implements ModulePosition{
	
	private LocalDateTime lastOpeningTime;
	
	/**
	 * 当前持仓合约
	 */
	private String currentUnifiedSymbolInPosition;
	
	private LinkedList<TradeField> tradeList = new LinkedList<>();
	
	/**
	 * TradeField --> Profit
	 */
	private Map<TradeField, AtomicInteger> positionProfitMap = new HashMap<>();
	
	public CtaModulePosition() {}
	
	public CtaModulePosition(List<TradeField> openTrades) {
		for(TradeField t : openTrades) {
			if(t.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
				tradeList.add(t);
				currentUnifiedSymbolInPosition = t.getContract().getUnifiedSymbol();
				positionProfitMap.put(t, new AtomicInteger(0));
				if(lastOpeningTime == null) {
					lastOpeningTime = LocalDateTime.ofEpochSecond(t.getTradeTimestamp() / 1000, 0, ZoneOffset.ofHours(8));
				}
			}
		}
	}

	@Override
	public List<TradeField> getOpenningTrade() {
		List<TradeField> result = new ArrayList<>();
		result.addAll(positionProfitMap.keySet());
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
		if(StringUtils.isNotBlank(currentUnifiedSymbolInPosition) 
				&& !StringUtils.equals(trade.getContract().getUnifiedSymbol(), currentUnifiedSymbolInPosition)) {
			log.warn("持仓合约 - [{}] 与成交合约 - [{}] 不一致，忽略更新", currentUnifiedSymbolInPosition, trade.getContract().getUnifiedSymbol());
			return;
		}
		currentUnifiedSymbolInPosition = trade.getContract().getUnifiedSymbol();
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Unkonwn) {
			throw new IllegalStateException("未定义成交类型");
		}
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
			//开仓成交
			positionProfitMap.putIfAbsent(trade, new AtomicInteger(0));
			tradeList.add(trade);
			if(lastOpeningTime == null) {
				lastOpeningTime = LocalDateTime.ofEpochSecond(trade.getTradeTimestamp() / 1000, 0, ZoneOffset.ofHours(8));
			}
		}else {
			//平仓成交
			int targetConsume = trade.getVolume();
			while(targetConsume > 0) {
				TradeField openingDeal = tradeList.pollFirst();
				positionProfitMap.remove(openingDeal);
				if(openingDeal.getVolume() <= targetConsume) {
					targetConsume -= openingDeal.getVolume();
				} else if(openingDeal.getVolume() > targetConsume) {
					int volDif = openingDeal.getVolume() - targetConsume;
					targetConsume = 0;
					TradeField restTrade = TradeField.newBuilder(openingDeal).setVolume(volDif).build();
					tradeList.offerFirst(restTrade);
					positionProfitMap.put(restTrade, new AtomicInteger(0));
				}
			}
		}
		
		if(positionProfitMap.size() == 0) {
			currentUnifiedSymbolInPosition = null;
			lastOpeningTime = null;
		}
	}

	@Override
	public int getPositionProfit() {
		return positionProfitMap.values()
				.stream()
				.reduce(0, (i, item) -> i + item.get(), (a, b) -> a + b);
	}

	@Override
	public OffsetFlagEnum getClosingOffsetFlag(String tradingDay) {
		TradeField trade = tradeList.peekFirst();
		// 非上期合约，直接用Close
		if(trade.getContract().getExchange() != ExchangeEnum.SHFE) {
			return OffsetFlagEnum.OF_Close;
		}
		
		// 对于上期合约，根据开仓时间计算
		if(StringUtils.equals(tradingDay, trade.getTradingDay())) {
			return OffsetFlagEnum.OF_CloseToday;
		}
		return OffsetFlagEnum.OF_CloseYesterday;
	}
}
