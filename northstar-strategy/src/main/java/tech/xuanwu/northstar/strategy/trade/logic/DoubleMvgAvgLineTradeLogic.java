package tech.xuanwu.northstar.strategy.trade.logic;

import tech.xuanwu.northstar.strategy.trade.DataRef;
import tech.xuanwu.northstar.strategy.trade.TradeLogic;
import tech.xuanwu.northstar.strategy.trade.indicator.MovingAverageIndicator;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 双均线交易策略
 * 
 * 多头信号：快线高于慢线
 * 空头信号：快线低于慢线
 * @author kevinhuangwl
 *
 */
public class DoubleMvgAvgLineTradeLogic implements TradeLogic{

	private MovingAverageIndicator fastLine;
	private MovingAverageIndicator slowLine;
	
	public DoubleMvgAvgLineTradeLogic(DataRef<BarField> dataRef, DataRef.PriceType priceType, int fastLineRef, int slowLineRef) {
		fastLine = new MovingAverageIndicator(dataRef, priceType, fastLineRef);
		slowLine = new MovingAverageIndicator(dataRef, priceType, slowLineRef);
	}

	@Override
	public void init() {
		fastLine.init();
		slowLine.init();		
	}

	@Override
	public Signal getCurrentSignal() {
		if(fastLine.getValue() > slowLine.getValue()) {
			return Signal.LONG;
		}
		if(fastLine.getValue() < slowLine.getValue()) {
			return Signal.SHORT;
		}
		return Signal.NONE;
	}
}
