package tech.xuanwu.northstar.persistance.po;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 一个交易对代表一次开平仓操作
 * @author kevinhuangwl
 *
 */
@Data
public class TradePair implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6412773715116430870L;

	private Trade openingTrade;
	private List<Trade> closingTrades;
	private double closingProfit;
	private int openPositionVol;
	
	public TradePair(){}
	
	public TradePair(TradeField trade) {
		if(trade.getOffsetFlag() != OffsetFlagEnum.OF_Open) {
			throw new IllegalStateException("期望入参为开仓成交");
		}
		openingTrade = Trade.convertFrom(trade);
		openPositionVol = trade.getVolume();
	}
	
	public TradeField closeWith(TradeField trade) {
		if(!openingTrade.getContractUnifiedSymbol().equals(trade.getContract().getUnifiedSymbol())) {
			throw new IllegalStateException("合约不匹配");
		}
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Open || trade.getOffsetFlag() == OffsetFlagEnum.OF_Unkonwn) {
			throw new IllegalStateException("期望入参为平仓成交");
		}
		int dir = trade.getDirection() == DirectionEnum.D_Buy ? 1 : -1;
		if(trade.getVolume() <= openPositionVol) {
			closingProfit += dir * (trade.getPrice() - openingTrade.getPrice()) * trade.getVolume() * trade.getContract().getMultiplier();
			openPositionVol -= trade.getVolume();
			closingTrades.add(Trade.convertFrom(trade));
			return null;
		}
		
		TradeField useTrade = trade.toBuilder().setVolume(openPositionVol).build();
		closingTrades.add(Trade.convertFrom(useTrade));
		TradeField restTrade = trade.toBuilder().setVolume(trade.getVolume() - openPositionVol).build();
		closingProfit += dir * (trade.getPrice() - openingTrade.getPrice()) * openPositionVol * trade.getContract().getMultiplier();
		openPositionVol = 0;
		return restTrade;
	}
	
	public double getClosingProfit() {
		return closingProfit;
	}
	
	public boolean isCloseOut() {
		return openPositionVol < 1;
	}
	
}
