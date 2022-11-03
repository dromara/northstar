package tech.quantit.northstar.strategy.api.utils.bar;

import java.util.ArrayList;
import java.util.List;
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
	
	protected Consumer<BarField> callback;
	
	protected ContractField bindedContract;
	
	protected BarField.Builder barBuilder;
	
	protected long curBarTimestamp;
	
	private List<BarField> barCache;
	
	private BarField lastBar;
	
	public BarMerger(int numOfMinPerBar, ContractField bindedContract, Consumer<BarField> callback) {
		this.numOfMinPerBar = numOfMinPerBar;
		this.callback = callback;
		this.bindedContract = bindedContract;
		this.barCache = new ArrayList<>(numOfMinPerBar);
	}
	
	public synchronized void updateBar(BarField bar) {
		if(!StringUtils.equals(bar.getUnifiedSymbol(), bindedContract.getUnifiedSymbol())) {
			return;
		}
		if(bar.getActionTimestamp() < curBarTimestamp) {
			return;
		}
		curBarTimestamp = bar.getActionTimestamp();
		
		boolean firstBarOfDay = Objects.isNull(lastBar) || !StringUtils.equals(lastBar.getTradingDay(), bar.getTradingDay());
		lastBar = bar;
		
		if(numOfMinPerBar == 1) {
			callback.accept(bar);
			return;
		}
		
		if(Objects.nonNull(barBuilder) && !StringUtils.equals(barBuilder.getTradingDay(), bar.getTradingDay())) {
			doGenerate();
		}
		
		// 忽略每天开盘首个K线的计数，使得合并数量对齐
		if(!firstBarOfDay) {	
			barCache.add(bar);
		}
				
		if(Objects.isNull(barBuilder)) {
			barBuilder = bar.toBuilder();
			return;
		}
		
		doMerger(bar);
		
		if(barCache.size() == numOfMinPerBar) {
			doGenerate();
		}
	}
	
	protected void doGenerate() {
		callback.accept(barBuilder.build());
		barBuilder = null;
		barCache.clear();
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
