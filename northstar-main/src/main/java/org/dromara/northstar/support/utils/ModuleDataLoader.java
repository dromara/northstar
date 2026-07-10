package org.dromara.northstar.support.utils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.kline.KlineGenerator;

import lombok.extern.slf4j.Slf4j;

/**
 * 模组数据加载器
 * 用于简化数据加载逻辑
 * 
 * 实现按周并发请求历史K线数据，并在加载时严格按K线时间顺序调用 KlineGenerator.updateBar。
 */
@Slf4j
public class ModuleDataLoader {

	private final IContract icontract;
	
	private final KlineGenerator klineGenerator;
	
	public ModuleDataLoader(IContract icontract, KlineGenerator klineGenerator) {
		this.icontract = icontract;
		this.klineGenerator = klineGenerator;
	}
	
	/**
	 * 加载指定周数的历史分钟K线数据，并顺序推送到 KlineGenerator。
	 * 
	 * @param weeks 需要加载的周数；小于等于 0 时不加载
	 */
	public void load(int weeks) {
		if (weeks <= 0) {
			return;
		}
		IDataSource dataSource = icontract.dataSource();
		if (dataSource == null) {
			throw new IllegalStateException(String.format("合约 [%s] 缺少数据源配置，无法加载历史数据", icontract.name()));
		}
		
		Contract contract = icontract.contract();
		LocalDate endDate = LocalDate.now();
		LocalDate startDate = endDate.minusWeeks(weeks);
		
		List<LocalDate[]> windows = splitWeeklyWindows(startDate, endDate);
		log.info("合约 [{}] 开始加载近 [{}] 周历史数据，共切分为 [{}] 个周窗口", icontract.name(), weeks, windows.size());
		
		try (ExecutorService exec = CommonUtils.newThreadPerTaskExecutor(ModuleDataLoader.class)) {
			List<CompletableFuture<List<Bar>>> futures = new ArrayList<>(windows.size());
			for (LocalDate[] window : windows) {
				LocalDate windowStart = window[0];
				LocalDate windowEnd = window[1];
				CompletableFuture<List<Bar>> future = CompletableFuture.supplyAsync(
						() -> dataSource.getData(contract, Interval.M1, windowStart, windowEnd).reversed(),
						exec);
				futures.add(future);
			}
			
			for (CompletableFuture<List<Bar>> future : futures) {
				List<Bar> bars = future.join();
				for (Bar bar : bars) {
					klineGenerator.updateBar(bar);
				}
			}
		}
	}
	
	/**
	 * 将日期区间切分为按周划分的小区间（左闭右开）。
	 */
	private List<LocalDate[]> splitWeeklyWindows(LocalDate startDate, LocalDate endDate) {
		List<LocalDate[]> windows = new ArrayList<>();
		LocalDate cursor = startDate;
		while (cursor.isBefore(endDate)) {
			LocalDate next = cursor.plusWeeks(1);
			if (next.isAfter(endDate)) {
				next = endDate;
			}
			windows.add(new LocalDate[] { cursor, next });
			cursor = next;
		}
		return windows;
	}
	
}
