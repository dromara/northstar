package org.dromara.northstar.gateway.playback;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.gateway.playback.model.DataFrame;

import lombok.extern.slf4j.Slf4j;

/**
 * 回放数据提供者
 * 负责回放数据的准备，包括预热数据与回放数据
 */
@Slf4j
public class PlaybackDataProvider {
	
	private final PlaybackContext context;
	private final PlaybackDataLoader dataLoader;
	private final ArrayBlockingQueue<DataFrame<Bar>> dataBuffer;
	private volatile LocalDateTime dataLoadCursor;
	private final AtomicBoolean isRunning = new AtomicBoolean();
	private final AtomicBoolean isLoadThreadActive = new AtomicBoolean();
	private final ExecutorService exec = CommonUtils.newThreadPerTaskExecutor(getClass());
	
	public PlaybackDataProvider(PlaybackContext context) {
		this.context = context;
		this.dataLoader = new PlaybackDataLoader(context.playbackContracts());
		this.dataBuffer = new ArrayBlockingQueue<>(1024 * 4);
		this.dataLoadCursor = LocalDateTime.of(context.preloadDate(), LocalTime.of(0, 0));
	}
	
	/**
	 * 加载预热数据
	 * @return 预热期间的数据帧列表
	 */
	public List<DataFrame<Bar>> loadPreloadData() throws InterruptedException {
		List<DataFrame<Bar>> result = new ArrayList<>();
		LocalDate date = context.preloadDate();
		while(date.isBefore(context.startDate())) {
			long startTime = CommonUtils.localDateTimeToMills(date.atStartOfDay());
			long endTime = CommonUtils.localDateTimeToMills(date.plusDays(3).atStartOfDay());
			result.addAll(dataLoader.loadData(startTime, endTime));
			date = date.plusDays(3);
		}
		return result;
	}
	
	/**
	 * 启动数据供应，从当前回放状态的时间开始
	 */
	public void startSupply() {
		isRunning.set(true);
		startLoadThreadIfNeeded();
	}
	
	/**
	 * 暂停数据供应
	 */
	public void pauseSupply() {
		isRunning.set(false);
	}
	
	/**
	 * 获取数据缓冲区
	 */
	public ArrayBlockingQueue<DataFrame<Bar>> getBuffer() {
		return dataBuffer;
	}
	
	/**
	 * 重置数据状态
	 */
	public void reset() {
		dataBuffer.clear();
		dataLoadCursor = LocalDateTime.of(context.preloadDate(), LocalTime.of(0, 0));
	}
	
	/**
	 * 设置运行时间
	 */
	public void setRunningTime(LocalDateTime runningDateTime) {
		dataBuffer.clear();
		dataLoadCursor = runningDateTime;
	}
	
	private void startLoadThreadIfNeeded() {
		if(isLoadThreadActive.compareAndSet(false, true)) {
			exec.execute(() -> {
				try {
					while(true) {
						if(!isRunning.get()) {
							Thread.sleep(100);
							continue;
						}
						LocalDateTime cursor = dataLoadCursor;
						if(cursor == null) {
							cursor = context.getPlaybackStatus().getRunningDateTime();
						}
						if(cursor.toLocalDate().isAfter(context.endDate())) {
							Thread.sleep(100);
							continue;
						}
						long startTime = CommonUtils.localDateTimeToMills(cursor);
						long endTime = CommonUtils.localDateTimeToMills(cursor.plusDays(1));
						List<DataFrame<Bar>> data = dataLoader.loadData(startTime, endTime);
						for(DataFrame<Bar> df : data) {
							while(!dataBuffer.offer(df, 100, TimeUnit.MILLISECONDS)) {
								if(!isRunning.get()) {
									Thread.sleep(100);
								}
							}
						}
						if(isRunning.get()) {
							dataLoadCursor = cursor.plusDays(1);
						}
					}
				} catch(InterruptedException e) {
					log.warn("数据加载线程被中断", e);
				}
			});
		}
	}
}
