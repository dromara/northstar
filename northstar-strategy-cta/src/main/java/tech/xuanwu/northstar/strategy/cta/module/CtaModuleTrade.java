package tech.xuanwu.northstar.strategy.cta.module;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.BeanUtils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.ModuleTrade;
import tech.xuanwu.northstar.strategy.common.model.DealRecord;
import tech.xuanwu.northstar.strategy.common.model.TradeDescription;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

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
	Map<String, List<TradeDescription>> openingTradeMap = new HashMap<>();
	Map<String, List<TradeDescription>> closingTradeMap = new HashMap<>();
	
	public CtaModuleTrade() {}
	
	public CtaModuleTrade(List<TradeDescription> originTradeList) {
		for(TradeDescription trade : originTradeList) {
			handleTrade(trade);
		}
	}
	
	@Override
	public List<DealRecord> getDealRecords() {
		List<DealRecord> result = new LinkedList<>();
		for(Entry<String, List<TradeDescription>> e : closingTradeMap.entrySet()) {
			String curSymbol = e.getKey();
			LinkedList<TradeDescription> tempClosingTrade = new LinkedList<>();
			tempClosingTrade.addAll(e.getValue());
			
			LinkedList<TradeDescription> tempOpeningTrade = new LinkedList<>();
			tempOpeningTrade.addAll(openingTradeMap.get(curSymbol));
			
			while(tempClosingTrade.size() > 0) {
				TradeDescription closingDeal = tempClosingTrade.pollFirst();
				TradeDescription openingDeal = tempOpeningTrade.pollFirst();
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
				int profit = (int) (priceDiff * closingDeal.getContractMultiplier() * vol);
				DealRecord deal = DealRecord.builder()
						.contractName(closingDeal.getContractName())
						.direction(dir)
						.dealTimestamp(closingDeal.getTradeTimestamp())
						.openPrice(openingDeal.getPrice())
						.closePrice(closingDeal.getPrice())
						.tradingDay(closingDeal.getTradingDay())
						.volume(vol)
						.closeProfit(profit)
						.build();
				result.add(deal);
				int volDiff = Math.abs(closingDeal.getVolume() - openingDeal.getVolume());
				TradeDescription restTrade = new TradeDescription();
				BeanUtils.copyProperties(closingDeal, restTrade);
				restTrade.setVolume(volDiff);
				// 平仓手数多于开仓手数,则需要拆分平仓成交
				if(closingDeal.getVolume() > openingDeal.getVolume()) {
					tempClosingTrade.offerFirst(restTrade);
				}
				// 平仓手数少于开仓手数,则需要拆分开仓成交
				else if(closingDeal.getVolume() < openingDeal.getVolume()) {
					tempOpeningTrade.offerFirst(restTrade);
				}
			}
		}
		
		return result;
	}

	@Override
	public void updateTrade(TradeDescription trade) {
		handleTrade(trade);
	}

	@Override
	public int getTotalCloseProfit() {
		List<DealRecord> dealList = getDealRecords();
		return dealList.stream().reduce(0, (d1, d2) -> d1 + d2.getCloseProfit(), (d1,d2) -> d1 + d2);
	}

	private void handleTrade(TradeDescription trade) {
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Unknown) {
			log.warn("未定义开平方向, {}", trade.toString());
			return;
		}
		String unifiedSymbol = trade.getUnifiedSymbol();
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Open) {
			openingTradeMap.putIfAbsent(unifiedSymbol, new LinkedList<>());
			openingTradeMap.get(unifiedSymbol).add(trade);
		} else {
			closingTradeMap.putIfAbsent(unifiedSymbol, new LinkedList<>());
			closingTradeMap.get(unifiedSymbol).add(trade);
		}
	}

}
