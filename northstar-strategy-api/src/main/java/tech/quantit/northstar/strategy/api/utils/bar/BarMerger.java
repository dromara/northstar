package tech.quantit.northstar.strategy.api.utils.bar;

import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 分钟线合成器
 * @author KevinHuangwl
 *
 */
public class BarMerger {
	
	private final int numOfMinPerBar;
	
	private int countBars;
	
	protected Consumer<BarField> callback;
	
	protected ContractField bindedContract;
	
	protected BarField.Builder barBuilder;
	
	protected long curBarTimestamp;
	
	public BarMerger(int numOfMinPerBar, ContractField bindedContract, Consumer<BarField> callback) {
		this.numOfMinPerBar = numOfMinPerBar;
		this.callback = callback;
		this.bindedContract = bindedContract;
	}
	
	public synchronized void updateBar(BarField bar) {
		if(!StringUtils.equals(bar.getUnifiedSymbol(), bindedContract.getUnifiedSymbol())) {
			return;
		}
		if(bar.getActionTimestamp() < curBarTimestamp) {
			return;
		}
		curBarTimestamp = bar.getActionTimestamp();
		if(numOfMinPerBar == 1) {
			callback.accept(bar);
			return;
		} else if(Objects.nonNull(barBuilder) && !StringUtils.equals(barBuilder.getTradingDay(), bar.getTradingDay())) {
			doGenerate();
		}
		countBars++;
		if(countBars == 1 || Objects.isNull(barBuilder)) {
			barBuilder = bar.toBuilder();
			return;
		}
		
		doMerger(bar);
		
		if(countBars == numOfMinPerBar) {
			doGenerate();
		}
	}
	
	protected void doGenerate() {
		callback.accept(barBuilder.build());
		barBuilder = null;
		countBars = 0;
	}
	
	protected void doMerger(BarField bar) {
		double high = barBuilder.getHighPrice();
		double low = barBuilder.getLowPrice();
		long vol = barBuilder.getVolume();
		long volumeDelta = barBuilder.getVolumeDelta();
		long numOfTrade = barBuilder.getNumTrades();
		long numOfTradeDelta = barBuilder.getNumTradesDelta();
		double turnover = barBuilder.getTurnover();
		double turnoverDelta = barBuilder.getTurnoverDelta();
		double openInterestDelta = barBuilder.getOpenInterestDelta();
		
		barBuilder
			.setActionDay(bar.getActionDay())
			.setActionTime(bar.getActionTime())
			.setActionTimestamp(bar.getActionTimestamp())
			.setTradingDay(bar.getTradingDay())
			.setClosePrice(bar.getClosePrice())
			.setHighPrice(Math.max(high, bar.getHighPrice()))
			.setLowPrice(Math.min(low, bar.getLowPrice()))
			.setVolume(vol + bar.getVolume())
			.setVolumeDelta(volumeDelta + bar.getVolumeDelta())
			.setOpenInterest(bar.getOpenInterest())
			.setOpenInterestDelta(openInterestDelta + bar.getOpenInterestDelta())
			.setNumTrades(numOfTrade + bar.getNumTrades())
			.setNumTradesDelta(numOfTradeDelta + bar.getNumTradesDelta())
			.setTurnover(turnover + bar.getTurnover())
			.setTurnoverDelta(turnoverDelta + bar.getTurnoverDelta());
	}

}
