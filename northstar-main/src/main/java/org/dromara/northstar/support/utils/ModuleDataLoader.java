package org.dromara.northstar.support.utils;

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
 * 按周切分时间窗口，以流水线方式（边加载边消费）按时间顺序加载历史K线数据，
 * 并在加载时严格按K线时间顺序调用 KlineGenerator.updateBar。
 * 
 * 为避免一次性缓存全部历史数据导致 OOM，任意时刻最多只缓存两个周窗口的数据
 * （一个正在消费，一个正在后台预取）。
 */
@Slf4j
public class ModuleDataLoader {

	private static final long WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000;

	private final IContract icontract;
	
	private final KlineGenerator klineGenerator;
	
	public ModuleDataLoader(IContract icontract, KlineGenerator klineGenerator) {
		this.icontract = icontract;
		this.klineGenerator = klineGenerator;
	}
	
	/**
	 * 加载指定周数的历史分钟K线数据（精确到当前时间为止），并顺序推送到 KlineGenerator。
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
		long endTime = System.currentTimeMillis();
		long startTime = endTime - weeks * WEEK_MILLIS;
		
		List<long[]> windows = splitWeeklyWindows(startTime, endTime);
		log.info("合约 [{}] 开始加载近 [{}] 周历史数据，共切分为 [{}] 个周窗口", icontract.name(), weeks, windows.size());
		
		try (ExecutorService exec = CommonUtils.newThreadPerTaskExecutor(ModuleDataLoader.class)) {
			// 预取第一个窗口，随后每消费完一个窗口立即释放其内存，并预取下一个窗口
			CompletableFuture<List<Bar>> prefetch = fetchAsync(dataSource, contract, windows.get(0), windows.size() == 1, exec);
			for (int i = 0; i < windows.size(); i++) {
				CompletableFuture<List<Bar>> current = prefetch;
				prefetch = i + 1 < windows.size() ? fetchAsync(dataSource, contract, windows.get(i + 1), i + 2 == windows.size(), exec) : null;
				for (Bar bar : current.join()) {
					klineGenerator.updateBar(bar);
				}
			}
		}
	}
	
	/**
	 * 异步加载一个时间窗口的数据。
	 * 
	 * 最后一个窗口的结束时间在请求发起时才取当前时间，
	 * 避免加载耗时较长时，尾部新产生的数据缺失。
	 */
	private CompletableFuture<List<Bar>> fetchAsync(IDataSource dataSource, Contract contract, long[] window, boolean isLastWindow, ExecutorService exec) {
		return CompletableFuture.supplyAsync(() -> {
			long end = isLastWindow ? Math.max(System.currentTimeMillis(), window[1]) : window[1];
			return dataSource.getData(contract, Interval.M1, window[0], end).reversed();
		}, exec);
	}
	
	/**
	 * 将时间区间切分为按周划分的小区间（左闭右开，单位：毫秒时间戳）。
	 */
	private List<long[]> splitWeeklyWindows(long startTime, long endTime) {
		List<long[]> windows = new ArrayList<>();
		for (long cursor = startTime; cursor < endTime;) {
			long next = Math.min(cursor + WEEK_MILLIS, endTime);
			windows.add(new long[] { cursor, next });
			cursor = next;
		}
		return windows;
	}
	
}
