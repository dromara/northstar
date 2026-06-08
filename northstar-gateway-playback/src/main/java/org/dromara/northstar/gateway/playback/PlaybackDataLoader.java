package org.dromara.northstar.gateway.playback;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.playback.model.DataFrame;
import org.dromara.northstar.gateway.playback.model.DataFrame.TimeEntry;

import lombok.extern.slf4j.Slf4j;

/**
 * 回放数据加载器
 * 相当于回放数据的生产者
 * @auth KevinHuangwl
 */
@Slf4j
public class PlaybackDataLoader {
	
	private final Collection<IContract> contracts;
	
	private final ExecutorService exec = Executors.newCachedThreadPool();
	
	public PlaybackDataLoader(Collection<IContract> contracts) {
		this.contracts = contracts;
	}
	
	// 把查询到的Bar数据按时间帧切片
	public List<DataFrame<Bar>> loadData(long startTime, long endTime) throws InterruptedException {
		LocalDateTime ldt = CommonUtils.millsToLocalDateTime(startTime);	// 要考虑周五夜盘的数据
		LocalDateTime endDateTime = CommonUtils.millsToLocalDateTime(endTime);
		LinkedList<DataFrame<Bar>> dataFrameList = new LinkedList<>();
		while(!ldt.isAfter(endDateTime)) {
			dataFrameList.add(new DataFrame<>(ldt, ldt.plusMinutes(1)));
			ldt = ldt.plusMinutes(1);
		}
		Map<TimeEntry, DataFrame<Bar>> timeDataMap = dataFrameList.stream().collect(Collectors.toMap(DataFrame::getTime, df -> df));
		
		CountDownLatch cdl = new CountDownLatch(contracts.size());
		for(IContract ic : contracts) {
			exec.execute(() -> {
				ic.dataSource().getData(ic.contract(), Interval.M1, startTime, endTime)
				.stream()
				.forEach(bar -> {
					long barTimestamp = bar.actionTimestamp();
					LocalDateTime start = null;
					LocalDateTime end = null;
					if(bar.contract().usingStartTimeInBarTime()) {
						start = CommonUtils.millsToLocalDateTime(barTimestamp);
						end = start.plusMinutes(1);
					} else {
						end = CommonUtils.millsToLocalDateTime(barTimestamp);
						start = end.minusMinutes(1);
					}
					TimeEntry timeEntry = TimeEntry.builder().start(start).end(end).build();
					if(timeDataMap.containsKey(timeEntry)) {
						timeDataMap.get(timeEntry).add(bar);
					}
				});
				cdl.countDown();
			});
		}
		cdl.await();
		return dataFrameList.stream().filter(df -> !df.items().isEmpty()).toList();
	}
	
}
