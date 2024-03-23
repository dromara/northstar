package org.dromara.northstar.gateway.mktdata;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;

/**
 * 数据加载器
 * 负责对查询范围做切片，适应缓存优化处理
 * @auth KevinHuangwl
 */
public class DataSourceDataLoader {
	
	private IDataSource ds;
	
	public DataSourceDataLoader(IDataSource ds) {
		this.ds = ds;
	}

	public void loadMinutelyData(Contract contract, LocalDate startDate, LocalDate endDate, Consumer<List<Bar>> sectionCallback) {
		splitByWeek(startDate, endDate, (start, end) -> sectionCallback.accept(ds.getMinutelyData(contract, start, end)));
	}
	
	public void loadQuarterlyData(Contract contract, LocalDate startDate, LocalDate endDate, Consumer<List<Bar>> sectionCallback) {
		splitByMonth(startDate, endDate, (start, end) -> sectionCallback.accept(ds.getQuarterlyData(contract, start, end)));
	}
	
	public void loadHourlyData(Contract contract, LocalDate startDate, LocalDate endDate, Consumer<List<Bar>> sectionCallback) {
		splitByMonth(startDate, endDate, (start, end) -> sectionCallback.accept(ds.getHourlyData(contract, start, end)));
	}
	
	public void loadDailyData(Contract contract, LocalDate startDate, LocalDate endDate, Consumer<List<Bar>> sectionCallback) {
		splitByYear(startDate, endDate, (start, end) -> sectionCallback.accept(ds.getDailyData(contract, start, end)));
	}
	
	private void splitByWeek(LocalDate startDate, LocalDate endDate, BiConsumer<LocalDate, LocalDate> queryExecutor) {
		LocalDate date = startDate;
		while(!date.isAfter(endDate)) {
			LocalDate start = date;
			LocalDate endOfThisWeek = date.plusDays(7L - date.getDayOfWeek().getValue());
			LocalDate end = endDate.isBefore(endOfThisWeek) ? endDate : endOfThisWeek;
			queryExecutor.accept(start, end);
			date = end.plusDays(1);
		}
	}
	
	private void splitByMonth(LocalDate startDate, LocalDate endDate, BiConsumer<LocalDate, LocalDate> queryExecutor) {
		LocalDate date = startDate;
		while(!date.isAfter(endDate)) {
			LocalDate start = date;
			LocalDate endOfThisMonth = date.plusMonths(1).withDayOfMonth(1).minusDays(1);
			LocalDate end = endDate.isBefore(endOfThisMonth) ? endDate : endOfThisMonth;
			queryExecutor.accept(start, end);
			date = end.plusDays(1);
		}
	}
	
	private void splitByYear(LocalDate startDate, LocalDate endDate, BiConsumer<LocalDate, LocalDate> queryExecutor) {
		LocalDate date = startDate;
		while(!date.isAfter(endDate)) {
			LocalDate start = date;
			LocalDate endOfThisYear = LocalDate.of(date.getYear(), 12, 31);
			LocalDate end = endDate.isBefore(endOfThisYear) ? endDate : endOfThisYear;
			queryExecutor.accept(start, end);
			date = end.plusDays(1);
		}
	}
}
