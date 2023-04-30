package org.dromara.northstar.common.utils;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.DateTimeConstant;

import cn.hutool.core.date.LocalDateTimeUtil;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * K线工具
 * @author KevinHuangwl
 *
 */
public class BarUtils {

	/**
	 * 合成周线列表
	 * 以每周第一根K线为基准合成
	 * @param srcBarList
	 * @return
	 */
	public static List<BarField> mergeWeeklyBar(List<BarField> srcBarList){
		LinkedList<BarField.Builder> resultList = new LinkedList<>();
		String yearlyWeek = null;
		for(BarField bar : srcBarList) {
			LocalDate date = LocalDate.parse(bar.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
			String yearlyWeekOfDate = String.format("%d%d", date.getYear(), LocalDateTimeUtil.weekOfYear(date));
			if(!StringUtils.equals(yearlyWeekOfDate, yearlyWeek)) {
				doCalculateDelta(resultList);
				resultList.add(BarField.newBuilder(bar));
				yearlyWeek = yearlyWeekOfDate;
				continue;
			}
			BarField.Builder lastBar = resultList.peekLast();
			lastBar.setTradingDay(bar.getTradingDay());
			lastBar.setActionDay(bar.getActionDay());
			lastBar.setActionTime(bar.getActionTime());
			lastBar.setActionTimestamp(bar.getActionTimestamp());
			lastBar.setHighPrice(Math.max(lastBar.getHighPrice(), bar.getHighPrice()));
			lastBar.setLowPrice(Math.min(lastBar.getLowPrice(), bar.getLowPrice()));
			lastBar.setClosePrice(bar.getClosePrice());
			lastBar.setVolume(lastBar.getVolume() + bar.getVolume());
			lastBar.setTurnover(lastBar.getTurnover() + bar.getTurnover());
			lastBar.setNumTrades(lastBar.getNumTrades() + bar.getNumTrades());
			lastBar.setOpenInterest(bar.getOpenInterest());
		}
		doCalculateDelta(resultList);
		
		return resultList.stream().map(BarField.Builder::build).toList();
	}
	
	private static void doCalculateDelta(List<BarField.Builder> resultList) {
		if(resultList.size() > 1) {
			BarField.Builder bb0 = resultList.get(resultList.size() - 1);
			BarField.Builder bb1 = resultList.get(resultList.size() - 2);
			bb0.setVolumeDelta(bb0.getVolume() - bb1.getVolume());
			bb0.setOpenInterestDelta(bb0.getOpenInterest() - bb1.getOpenInterest());
			bb0.setNumTradesDelta(bb0.getNumTrades() - bb1.getNumTrades());
			bb0.setTurnoverDelta(bb0.getTurnover() - bb1.getTurnover());
		}
	}
	
	public static boolean isEndOfTheTradingDay(BarField bar) {
		if(bar.getGatewayId().startsWith("CTP")) {
			boolean absEndTime = bar.getActionTime().startsWith("15:15");
			boolean mostEndTime = bar.getActionTime().startsWith("15:00") && !(bar.getUnifiedSymbol().startsWith("T") && bar.getUnifiedSymbol().contains(ExchangeEnum.CFFEX.toString()));
			return mostEndTime || absEndTime;
		}
		return false;
	}
}
