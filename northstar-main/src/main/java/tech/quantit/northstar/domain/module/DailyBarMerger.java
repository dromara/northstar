package tech.quantit.northstar.domain.module;

import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 日线合成器
 * @author KevinHuangwl
 *
 */
public class DailyBarMerger extends BarMerger{
	
	public DailyBarMerger(ContractField bindedContract, Consumer<BarField> callback) {
		super(0, bindedContract, callback);
	}

	@Override
	public void updateBar(BarField bar) {
		if(!StringUtils.equals(bar.getUnifiedSymbol(), bindedContract.getUnifiedSymbol())) {
			return;
		}
		
		if(Objects.nonNull(barBuilder) && !StringUtils.equals(barBuilder.getTradingDay(), bar.getTradingDay())) {
			doGenerate();
		}
		
		if(Objects.isNull(barBuilder)) {
			barBuilder = bar.toBuilder();
			return;
		}
		
		double high = barBuilder.getHighPrice();
		double low = barBuilder.getLowPrice();
		long volumeDelta = barBuilder.getVolumeDelta();
		long numOfTradeDelta = barBuilder.getNumTradesDelta();
		double openInterestDelta = barBuilder.getOpenInterestDelta();
		double turnoverDelta = barBuilder.getTurnoverDelta();
		
		barBuilder
			.setActionDay(bar.getActionDay())
			.setActionTime(bar.getActionTime())
			.setActionTimestamp(bar.getActionTimestamp())
			.setClosePrice(bar.getClosePrice())
			.setHighPrice(Math.max(high, bar.getHighPrice()))
			.setLowPrice(Math.min(low, bar.getLowPrice()))
			.setVolume(bar.getVolume())
			.setVolumeDelta(volumeDelta + bar.getVolumeDelta())
			.setOpenInterest(bar.getOpenInterest())
			.setOpenInterestDelta(openInterestDelta + bar.getOpenInterestDelta())
			.setNumTrades(bar.getNumTrades())
			.setNumTradesDelta(numOfTradeDelta + bar.getNumTradesDelta())
			.setTurnover(bar.getTurnover())
			.setTurnoverDelta(turnoverDelta + bar.getTurnoverDelta());
	}
	
}
