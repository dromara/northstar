package org.dromara.northstar.gateway.mktdata;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.gateway.utils.DataLoadUtil;

/**
 * 数据加载器
 * 负责对查询范围做切片，适应缓存优化处理
 * @auth KevinHuangwl
 */
public class DataSourceDataLoader {
	
	private IDataSource ds;
	
	private DataLoadUtil util = new DataLoadUtil();
	
	public DataSourceDataLoader(IDataSource ds) {
		this.ds = ds;
	}

	public void loadMinutelyData(Contract contract, LocalDate startDate, LocalDate endDate, Consumer<List<Bar>> sectionCallback) {
		util.splitByWeek(startDate, endDate, (start, end) -> sectionCallback.accept(ds.getMinutelyData(contract, start, end)));
	}
	
	public void loadQuarterlyData(Contract contract, LocalDate startDate, LocalDate endDate, Consumer<List<Bar>> sectionCallback) {
		util.splitByMonth(startDate, endDate, (start, end) -> sectionCallback.accept(ds.getQuarterlyData(contract, start, end)));
	}
	
	public void loadHourlyData(Contract contract, LocalDate startDate, LocalDate endDate, Consumer<List<Bar>> sectionCallback) {
		util.splitByMonth(startDate, endDate, (start, end) -> sectionCallback.accept(ds.getHourlyData(contract, start, end)));
	}
	
	public void loadDailyData(Contract contract, LocalDate startDate, LocalDate endDate, Consumer<List<Bar>> sectionCallback) {
		util.splitByYear(startDate, endDate, (start, end) -> sectionCallback.accept(ds.getDailyData(contract, start, end)));
	}
	
}
