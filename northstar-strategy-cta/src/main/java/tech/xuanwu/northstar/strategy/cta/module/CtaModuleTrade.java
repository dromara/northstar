package tech.xuanwu.northstar.strategy.cta.module;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.ModuleTrade;
import tech.xuanwu.northstar.strategy.common.model.DealRecord;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 用于记录模组的所有成交记录,并以此计算得出相应的每次开平仓盈亏,以及开平仓配对
 * @author KevinHuangwl
 *
 */
@Slf4j
public class CtaModuleTrade implements ModuleTrade {

	/**
	 * unifiedSymbol --> tradeList
	 */
	Map<String, List<TradeField>> openingTradeMap = new HashMap<>();
	Map<String, List<TradeField>> closingTradeMap = new HashMap<>();
	
	public CtaModuleTrade() {}
	
	public CtaModuleTrade(List<TradeField> originTradeList) {
		for(TradeField trade : originTradeList) {
			handleTrade(trade);
		}
	}
	
	@Override
	public List<DealRecord> getDealRecords() {
		List<DealRecord> result = new LinkedList<>();
		for(Entry<String, List<TradeField>> e : closingTradeMap.entrySet()) {
			String curSymbol = e.getKey();
			LinkedList<TradeField> tempClosingTrade = new LinkedList<>();
			tempClosingTrade.addAll(e.getValue());
			
			LinkedList<TradeField> tempOpeningTrade = new LinkedList<>();
			tempOpeningTrade.addAll(openingTradeMap.get(curSymbol));
			
			while(tempClosingTrade.size() > 0) {
				TradeField closingDeal = tempClosingTrade.peekFirst();
				TradeField openingDeal = tempOpeningTrade.peekFirst();
				if(closingDeal == null || openingDeal == null 
						|| closingDeal.getTradeTimestamp() < openingDeal.getTradeTimestamp()) {
					throw new IllegalStateException("存在异常的平仓合约找不到对应的开仓合约");
				}
				PositionDirectionEnum dir = openingDeal.getDirection() == DirectionEnum.D_Buy ? PositionDirectionEnum.PD_Long 
						: openingDeal.getDirection() == DirectionEnum.D_Sell ? PositionDirectionEnum.PD_Short : PositionDirectionEnum.PD_Unknown;
				if(PositionDirectionEnum.PD_Unknown == dir) {
					throw new IllegalStateException("持仓方向不能确定");
				}
				int factor = PositionDirectionEnum.PD_Long == dir ? 1 : -1;
				double priceDiff = factor * (closingDeal.getPrice() - openingDeal.getPrice());
				int vol = Math.min(closingDeal.getVolume(), openingDeal.getVolume());
				int profit = (int) (priceDiff * closingDeal.getContract().getMultiplier() * vol);
				DealRecord deal = DealRecord.builder()
						.unifiedSymbol(curSymbol)
						.direction(dir)
						.dealTimestamp(closingDeal.getTradeTimestamp())
						.openPrice(openingDeal.getPrice())
						.closePrice(closingDeal.getPrice())
						.volume(vol)
						.closeProfit(profit)
						.build();
				result.add(deal);
				int volDiff = Math.abs(closingDeal.getVolume() - openingDeal.getVolume());
				// 平仓手数多于开仓手数,则需要拆分平仓成交
				if(closingDeal.getVolume() > openingDeal.getVolume()) {
					TradeField restTrade = TradeField.newBuilder(closingDeal).setVolume(volDiff).build();
					tempClosingTrade.offerFirst(restTrade);
				}
				// 平仓手数少于开仓手数,则需要拆分开仓成交
				else if(closingDeal.getVolume() < openingDeal.getVolume()) {
					TradeField restTrade = TradeField.newBuilder(openingDeal).setVolume(volDiff).build();
					tempOpeningTrade.offerFirst(restTrade);
				}
			}
		}
		
		return result;
	}

	@Override
	public void updateTrade(TradeField trade) {
		handleTrade(trade);
	}

	@Override
	public int getTotalCloseProfit() {
		List<DealRecord> dealList = getDealRecords();
		return dealList.stream().reduce(0, (d1, d2) -> d1 + d2.getCloseProfit(), (d1,d2) -> d1 + d2);
	}

	@Override
	public List<TradeField> getOriginRecords() {
		List<TradeField> result = new LinkedList<>();
		for(Entry<String, List<TradeField>> e : openingTradeMap.entrySet()) {
			result.addAll(e.getValue());
		}
		for(Entry<String, List<TradeField>> e : closingTradeMap.entrySet()) {
			result.addAll(e.getValue());
		}
		result.sort((a,b)-> a.getTradeTimestamp() < b.getTradeTimestamp() ? -1 : 1);
		return result;
	}
	
	private void handleTrade(TradeField trade) {
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Unkonwn) {
			log.warn("未定义开平方向, {}", trade.toString());
			return;
		}
		String unifiedSymbol = trade.getContract().getUnifiedSymbol();
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
			openingTradeMap.putIfAbsent(unifiedSymbol, new LinkedList<>());
			openingTradeMap.get(unifiedSymbol).add(trade);
		} else {
			closingTradeMap.putIfAbsent(unifiedSymbol, new LinkedList<>());
			closingTradeMap.get(unifiedSymbol).add(trade);
		}
	}

}
