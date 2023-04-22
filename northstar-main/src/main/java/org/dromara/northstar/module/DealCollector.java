package org.dromara.northstar.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.MessagePrinter;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
public class DealCollector {

	/* unifiedSymbol -> tradeList */
	private Map<String, LinkedList<TradeField>> buyTradeMap = new HashMap<>();
	private Map<String, LinkedList<TradeField>> sellTradeMap = new HashMap<>();
	
	private ClosingPolicy closingPolicy;
	
	private String moduleName;
	
	public DealCollector(String moduleName, ClosingPolicy closingPolicy) {
		this.closingPolicy = closingPolicy;
		this.moduleName = moduleName;
	}
	
	public Optional<List<ModuleDealRecord>> onTrade(TradeField trade) {
		// 开仓处理
		if(FieldUtils.isOpen(trade.getOffsetFlag())) {
			getOpenMap(trade.getDirection()).putIfAbsent(trade.getContract().getUnifiedSymbol(), new LinkedList<>());
			getOpenMap(trade.getDirection()).get(trade.getContract().getUnifiedSymbol()).offer(trade);
			return Optional.empty();
		}

		// 平仓处理
		List<ModuleDealRecord> resultList = new ArrayList<>();
		if(closingPolicy == ClosingPolicy.FIRST_IN_LAST_OUT) {
			while(true) {
				TradeField openTrade = getCloseMap(trade.getDirection()).get(trade.getContract().getUnifiedSymbol()).pollLast();
				if(openTrade.getVolume() < trade.getVolume()) {
					TradeField matchTrade = trade.toBuilder().setVolume(openTrade.getVolume()).build();
					trade = trade.toBuilder().setVolume(trade.getVolume() - openTrade.getVolume()).build();
					resultList.add(makeRecord(openTrade, matchTrade));
				} else if (openTrade.getVolume() > trade.getVolume()) {
					TradeField matchTrade = openTrade.toBuilder().setVolume(trade.getVolume()).build();
					TradeField restTrade = openTrade.toBuilder().setVolume(openTrade.getVolume() - trade.getVolume()).build();
					resultList.add(makeRecord(matchTrade, trade));
					getCloseMap(trade.getDirection()).get(trade.getContract().getUnifiedSymbol()).offerLast(restTrade);
					return Optional.of(resultList);
				} else {
					resultList.add(makeRecord(openTrade, trade));
					return Optional.of(resultList);
				}
			}
		} else {
			while(true) {
				LinkedList<TradeField> openTradeList = getCloseMap(trade.getDirection()).get(trade.getContract().getUnifiedSymbol());
				if(openTradeList == null || openTradeList.isEmpty()) {
					log.warn("异常平仓：{}", MessagePrinter.print(trade));
					throw new IllegalStateException("不存在该成交对应的开仓记录");
				}
				TradeField openTrade = openTradeList.pollFirst(); 
				if(openTrade.getVolume() < trade.getVolume()) {
					TradeField matchTrade = trade.toBuilder().setVolume(openTrade.getVolume()).build();
					trade = trade.toBuilder().setVolume(trade.getVolume() - openTrade.getVolume()).build();
					resultList.add(makeRecord(openTrade, matchTrade));
				} else if (openTrade.getVolume() > trade.getVolume()) {
					TradeField matchTrade = openTrade.toBuilder().setVolume(trade.getVolume()).build();
					TradeField restTrade = openTrade.toBuilder().setVolume(openTrade.getVolume() - trade.getVolume()).build();
					resultList.add(makeRecord(matchTrade, trade));
					getCloseMap(trade.getDirection()).get(trade.getContract().getUnifiedSymbol()).offerFirst(restTrade);
					return Optional.of(resultList);
				} else {
					resultList.add(makeRecord(openTrade, trade));
					return Optional.of(resultList);
				}
			}
		}
	}
	
	private ModuleDealRecord makeRecord(TradeField openTrade, TradeField closeTrade) {
		ContractField contract = closeTrade.getContract();
		int factor = FieldUtils.directionFactor(openTrade.getDirection());
		double dealProfit = factor * (closeTrade.getPrice() - openTrade.getPrice()) * contract.getMultiplier() * closeTrade.getVolume();
		return ModuleDealRecord.builder()
				.moduleName(moduleName)
				.moduleAccountId(closeTrade.getGatewayId())
				.contractName(contract.getName())
				.openTrade(openTrade.toByteArray())
				.closeTrade(closeTrade.toByteArray())
				.dealProfit(dealProfit)
				.build();
	}
	
	private Map<String, LinkedList<TradeField>> getOpenMap(DirectionEnum dir){
		return switch(dir) {
		case D_Buy -> buyTradeMap;
		case D_Sell -> sellTradeMap;
		default -> throw new IllegalArgumentException("Unexpected value: " + dir);
		};
	}
	
	private Map<String, LinkedList<TradeField>> getCloseMap(DirectionEnum dir){
		return switch(dir) {
		case D_Buy -> sellTradeMap;
		case D_Sell -> buyTradeMap;
		default -> throw new IllegalArgumentException("Unexpected value: " + dir);
		};
	}
}
