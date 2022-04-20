package tech.quantit.northstar.common.utils;

import java.util.List;

import xyz.redtorch.pb.CoreField.BarField;

public class BarUtils {

	public BarUtils() {}
	
	public static BarField merge(List<BarField> bars) {
		if(bars.isEmpty()) {
			throw new IllegalArgumentException("空BAR集合无法合成新BAR");
		}
		BarField lastBar = bars.get(bars.size() - 1);
		BarField firstBar = bars.get(0);
		double open = firstBar.getOpenPrice();
		double close = bars.get(bars.size() - 1).getClosePrice();
		double high = bars.stream().mapToDouble(BarField::getHighPrice).max().getAsDouble();
		double low = bars.stream().mapToDouble(BarField::getLowPrice).min().getAsDouble();
		long vol = bars.stream().mapToLong(BarField::getVolumeDelta).sum();
		double openInterest = bars.stream().mapToDouble(BarField::getOpenInterestDelta).sum();
		long numOfTrade = bars.stream().mapToLong(BarField::getNumTradesDelta).sum();
		double turnover = bars.stream().mapToDouble(BarField::getTurnoverDelta).sum();
		return BarField.newBuilder(firstBar)
				.setClosePrice(close)
				.setOpenPrice(open)
				.setHighPrice(high)
				.setLowPrice(low)
				.setVolume(lastBar.getVolume())
				.setVolumeDelta(vol)
				.setNumTrades(lastBar.getNumTrades())
				.setNumTradesDelta(numOfTrade)
				.setTurnover(lastBar.getTurnover())
				.setTurnoverDelta(turnover)
				.setOpenInterest(lastBar.getOpenInterest())
				.setOpenInterestDelta(openInterest)
				.build();
	}
}
