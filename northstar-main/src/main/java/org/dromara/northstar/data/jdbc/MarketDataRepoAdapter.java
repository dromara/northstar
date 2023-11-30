package org.dromara.northstar.data.jdbc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.data.jdbc.entity.BarDO;
import org.dromara.northstar.gateway.IContract;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MarketDataRepoAdapter implements IMarketDataRepository{

	private MarketDataRepository delegate;
	
	public MarketDataRepoAdapter(MarketDataRepository delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public void insert(Bar bar) {
		delegate.save(BarDO.builder()
				.unifiedSymbol(bar.contract().unifiedSymbol())
				.tradingDay(bar.tradingDay().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.bar(bar)
				.build());
	}

	@Override
	public List<Bar> loadBars(IContract contract, LocalDate startDate, LocalDate endDate0) {
		log.debug("加载 [{}] 历史行情数据：{} -> {}", contract.contract().unifiedSymbol(), startDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), endDate0.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		LocalDate today = LocalDate.now();
		LocalDate endDate = today.isAfter(endDate0) ? endDate0 : today;
		LinkedList<Bar> resultList = new LinkedList<>();
		IDataSource dataServiceDelegate = contract.dataSource();
		Contract cf = contract.contract();
		if(endDate.isAfter(startDate)) {
			List<Bar> list = dataServiceDelegate.getMinutelyData(cf, startDate, endDate)
					.stream()
					.sorted((a, b) -> a.actionTimestamp() < b.actionTimestamp() ? -1 : 1)
					.toList();
			resultList.addAll(list);
		}
		if(today.isAfter(endDate0))	return resultList; 
		
		LocalDate localQueryDate = today;
		if(resultList.isEmpty()) {
			while(resultList.isEmpty() && endDate0.isAfter(localQueryDate)) {
				resultList.addAll(findBarData(localQueryDate, cf.unifiedSymbol()));
				localQueryDate = localQueryDate.plusDays(1);
			}
		} else {			
			if(resultList.peekLast().tradingDay().equals(today)
					|| today.getDayOfWeek().getValue() > 5) {
				do {					
					localQueryDate = localQueryDate.plusDays(1);
				} while(localQueryDate.getDayOfWeek().getValue() > 5);
				resultList.addAll(findBarData(localQueryDate, cf.unifiedSymbol()));
			} else {
				resultList.addAll(findBarData(localQueryDate, cf.unifiedSymbol()));
			}
		}
		
		return resultList;
	}
	
	private List<Bar> findBarData(LocalDate date, String unifiedSymbol){
		String tradingDay = date.format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
		log.debug("加载 [{}] 本地行情数据：{}", unifiedSymbol, tradingDay);
		try {
			return delegate.findByUnifiedSymbolAndTradingDay(unifiedSymbol, tradingDay)
					.stream()
					.map(BarDO::getBar)
					.filter(Objects::nonNull)
					.toList();
		} catch (Exception e) {
			log.error("{}", e.getMessage());
			return Collections.emptyList();
		}
	}
	
	@Override
	public List<Bar> loadDailyBars(IContract contract, LocalDate startDate, LocalDate endDate) {
		IDataSource dataServiceDelegate = contract.dataSource();
		try {
			return dataServiceDelegate.getDailyData(contract.contract(), startDate, endDate);
		} catch (Exception e) {
			log.error("{}", e.getMessage());
			return Collections.emptyList();
		}
	}
	
}
