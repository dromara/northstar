package tech.quantit.northstar.domain.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.ClosingPolicy;
import tech.quantit.northstar.common.model.ModuleDealRecord;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.common.utils.MessagePrinter;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
public class DealCollector {

	private Map<ContractField, LinkedList<TradeField>> buyTradeMap = new HashMap<>();
	private Map<ContractField, LinkedList<TradeField>> sellTradeMap = new HashMap<>();
	
	private ClosingPolicy closingPolicy;
	
	private String moduleName;
	
	public DealCollector(String moduleName, ClosingPolicy closingPolicy) {
		this.closingPolicy = closingPolicy;
		this.moduleName = moduleName;
	}
	
	public Optional<List<ModuleDealRecord>> onTrade(TradeField trade) {
		// 开仓处理
		if(FieldUtils.isOpen(trade.getOffsetFlag())) {
			getOpenMap(trade.getDirection()).putIfAbsent(trade.getContract(), new LinkedList<>());
			getOpenMap(trade.getDirection()).get(trade.getContract()).offer(trade);
			return Optional.empty();
		}

		// 平仓处理
		List<ModuleDealRecord> resultList = new ArrayList<>();
		if(closingPolicy == ClosingPolicy.PRIOR_TODAY) {
			while(true) {
				TradeField openTrade = getCloseMap(trade.getDirection()).get(trade.getContract()).pollLast();
				if(openTrade.getVolume() < trade.getVolume()) {
					TradeField matchTrade = trade.toBuilder().setVolume(openTrade.getVolume()).build();
					trade = trade.toBuilder().setVolume(trade.getVolume() - openTrade.getVolume()).build();
					resultList.add(makeRecord(openTrade, matchTrade));
				} else if (openTrade.getVolume() > trade.getVolume()) {
					TradeField matchTrade = openTrade.toBuilder().setVolume(trade.getVolume()).build();
					TradeField restTrade = openTrade.toBuilder().setVolume(openTrade.getVolume() - trade.getVolume()).build();
					resultList.add(makeRecord(matchTrade, trade));
					getCloseMap(trade.getDirection()).get(trade.getContract()).offerLast(restTrade);
					return Optional.of(resultList);
				} else {
					resultList.add(makeRecord(openTrade, trade));
					return Optional.of(resultList);
				}
			}
		} else {
			while(true) {
				LinkedList<TradeField> openTradeList = getCloseMap(trade.getDirection()).get(trade.getContract());
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
					getCloseMap(trade.getDirection()).get(trade.getContract()).offerFirst(restTrade);
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
	
	private Map<ContractField, LinkedList<TradeField>> getOpenMap(DirectionEnum dir){
		return switch(dir) {
		case D_Buy -> buyTradeMap;
		case D_Sell -> sellTradeMap;
		default -> throw new IllegalArgumentException("Unexpected value: " + dir);
		};
	}
	
	private Map<ContractField, LinkedList<TradeField>> getCloseMap(DirectionEnum dir){
		return switch(dir) {
		case D_Buy -> sellTradeMap;
		case D_Sell -> buyTradeMap;
		default -> throw new IllegalArgumentException("Unexpected value: " + dir);
		};
	}
}
