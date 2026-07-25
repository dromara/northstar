package org.dromara.northstar.gateway.playback;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.atomic.AtomicBoolean;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.gateway.playback.PlaybackStatus.State;
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
	private final AtomicBoolean hasTerminated = new AtomicBoolean();
	private final AtomicBoolean isLoadThreadActive = new AtomicBoolean();
	private final AtomicBoolean preloadSupplyDone = new AtomicBoolean();
	
	private static final int MAX_LOAD_RETRY = 3;
	// 使用平台线程池（守护线程），避免虚拟线程无法被 jstack 排查，以及非守护线程阻塞 JVM 退出
	private final ExecutorService exec = Executors.newCachedThreadPool(CommonUtils.platformThreadFactory(getClass()));
	
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
	public ArrayBlockingQueue<DataFrame<Bar>> loadPreloadData() {
		ArrayBlockingQueue<DataFrame<Bar>> preloadBuffer = new ArrayBlockingQueue<>(1024);
		preloadSupplyDone.set(false);
		exec.execute(() -> {
			// 将预热时间段按3天一个窗口拆分，多线程并发加载数据，再按窗口的时间顺序依次写入缓冲区，保证预热数据按自然顺序回放
			List<LocalDate> windowStartDates = new ArrayList<>();
			for(LocalDate date = context.preloadDate(); date.isBefore(context.startDate()); date = date.plusDays(3)) {
				windowStartDates.add(date);
			}
			ExecutorService loadPool = Executors.newFixedThreadPool(Math.max(1, Math.min(4, windowStartDates.size())),
					CommonUtils.platformThreadFactory(getClass()));
			try {
				List<CompletableFuture<List<DataFrame<Bar>>>> futures = windowStartDates.stream()
						.map(date -> CompletableFuture.supplyAsync(() -> {
							long startTime = CommonUtils.localDateTimeToMills(date.atStartOfDay());
							long endTime = CommonUtils.localDateTimeToMills(date.plusDays(3).atStartOfDay());
							try {
								return dataLoader.loadData(startTime, endTime);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								throw new CompletionException(e);
							}
						}, loadPool))
						.toList();
				// 按时间顺序逐窗口取结果写入缓冲区，确保预热顺序与并发加载的完成顺序无关
				for(CompletableFuture<List<DataFrame<Bar>>> future : futures) {
					for(DataFrame<Bar> df : future.join()) {
						while(!preloadBuffer.offer(df, 100, TimeUnit.MILLISECONDS)) {
							if(context.getPlaybackStatus().getState() != State.PRELOADING) {
								preloadBuffer.clear();
								return;
							}
						}
					}
				}
			} catch(InterruptedException e) {
				log.warn("加载中断", e);
				Thread.currentThread().interrupt();
			} catch(CompletionException e) {
				log.warn("预热数据加载异常", e);
			} finally {
				loadPool.shutdown();
				preloadSupplyDone.set(true);
			}
		});
		
		return preloadBuffer;
	}
	
	/**
	 * 启动数据供应，从当前回放状态的时间开始
	 */
	public void startSupply() {
		hasTerminated.set(false);
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
	 * 数据供应是否已耗尽（加载线程已退出且缓冲区已空，即不会再有新数据）
	 */
	public boolean isExhausted() {
		return !isLoadThreadActive.get() && dataBuffer.isEmpty();
	}
	
	/**
	 * 是否还有未加载的数据（加载游标未超过回放结束日）
	 */
	public boolean hasMoreData() {
		LocalDateTime cursor = dataLoadCursor;
		return cursor == null || !cursor.toLocalDate().isAfter(context.endDate());
	}
	
	/**
	 * 预热数据供应是否已完成（无论是否有数据，加载任务已结束）
	 */
	public boolean isPreloadSupplyDone() {
		return preloadSupplyDone.get();
	}
	
	/**
	 * 设置运行时间
	 */
	public void setRunningTime(LocalDateTime runningDateTime) {
		hasTerminated.set(true);
		dataLoadCursor = runningDateTime;
		// 等待加载线程退出，避免忙等空转（虚拟线程空转会长时间占用载体线程）
		while(isLoadThreadActive.get()) {
			LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
		}
		// 同步清空缓冲区，避免旧数据残留（加载线程终止时也会清理，此处保证方法返回时缓冲区已为空）
		dataBuffer.clear();
		startLoadThreadIfNeeded();
	}
	
	/**
	 * 带重试的数据加载。加载线程不能因异常退出，否则消费方会因缓冲区再无数据而永久卡住。
	 * 重试多次仍失败时跳过当天数据，避免回放永久停滞在同一天。
	 * @return 数据帧列表；返回 null 表示加载期间收到终止信号
	 */
	private List<DataFrame<Bar>> loadDataWithRetry(LocalDateTime cursor, long startTime, long endTime) throws InterruptedException {
		for(int attempt = 1; attempt <= MAX_LOAD_RETRY; attempt++) {
			if(hasTerminated.get()) {
				return null;
			}
			try {
				return dataLoader.loadData(startTime, endTime);
			} catch (InterruptedException e) {
				throw e;
			} catch (Exception e) {
				log.error("加载 [{}] 的回放数据异常（第{}/{}次）", cursor.toLocalDate(), attempt, MAX_LOAD_RETRY, e);
				Thread.sleep(1000);
			}
		}
		log.error("加载 [{}] 的回放数据多次失败，跳过当天数据继续回放", cursor.toLocalDate());
		return List.of();
	}
	
	private void startLoadThreadIfNeeded() {
		if(isLoadThreadActive.compareAndSet(false, true)) {
			exec.execute(() -> {
				try {
					while(true) {
						if(hasTerminated.get()) {
							return;
						}
						if(!isRunning.get()) {
							Thread.sleep(100);
							continue;
						}
						LocalDateTime cursor = dataLoadCursor;
						if(cursor == null) {
							cursor = context.getPlaybackStatus().getRunningDateTime();
						}
						if(cursor.toLocalDate().isAfter(context.endDate())) {
							// 回放数据已全部加载完毕，加载线程正常退出。恢复回放时可通过 startSupply/setRunningTime 重新启动
							log.debug("回放数据已加载完毕，加载线程退出");
							return;
						}
						long startTime = CommonUtils.localDateTimeToMills(cursor);
						long endTime = CommonUtils.localDateTimeToMills(cursor.plusDays(1));
						List<DataFrame<Bar>> data = loadDataWithRetry(cursor, startTime, endTime);
						if(data == null) {
							return;	// 加载过程中收到终止信号
						}
						for(DataFrame<Bar> df : data) {
							while(!dataBuffer.offer(df, 100, TimeUnit.MILLISECONDS)) {
								if(!isRunning.get()) {
									Thread.sleep(100);
								}
								if(hasTerminated.get()) {
									return;
								}
							}
						}
						if(isRunning.get()) {
							dataLoadCursor = cursor.plusDays(1);
						}
					}
				} catch(InterruptedException e) {
					log.warn("数据加载线程被中断", e);
				} finally {
					isLoadThreadActive.set(false);
					// 仅在强制重置（setRunningTime）时清空缓冲区；正常加载完毕退出时，缓冲区内可能仍有待消费的帧，不能清理
					if(hasTerminated.get()) {
						dataBuffer.clear();
					}
				}
			});
		}
	}
}
