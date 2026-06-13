package org.dromara.northstar.gateway.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.gateway.playback.model.PlaybackPrecision;
import org.dromara.northstar.gateway.playback.model.PlaybackSpeed;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * PlaybackContext 状态变更回调测试
 * 
 * 本测试主要验证 PlaybackContext 在各种状态流转时，均会通过 setOnStatusChangeCallback
 * 注册的回调通知调用方。
 */
class PlaybackContextTest {

	private static final String START_DATE = "20240102";
	private static final String END_DATE = "20240103";
	private static final String PRE_START_DATE = "20240101";

	private FastEventEngine feEngine;
	private MarketCenter mktCenter;
	private PlaybackGatewayAdapter gateway;
	private PlaybackContext context;

	@BeforeEach
	void setUp() {
		feEngine = mock(FastEventEngine.class);
		mktCenter = mock(MarketCenter.class);
	}

	@AfterEach
	void tearDown() {
		// 确保测试过程中创建的线程池都被关闭，避免测试用例之间互相影响
		if (context != null) {
			shutdownExecutor(context);
		}
	}

	@Test
	void shouldInvokeCallbackWhenResettingToInitial() {
		initContext(PRE_START_DATE);

		Consumer<PlaybackStatus> callback = mockCallback();
		context.setOnStatusChangeCallback(callback);

		context.reset();

		assertStatesContain(callback, PlaybackStatus.State.INITIAL);
	}

	@Test
	void shouldInvokeCallbackWhenResettingToPreloaded() {
		initContext(null);

		Consumer<PlaybackStatus> callback = mockCallback();
		context.setOnStatusChangeCallback(callback);

		context.reset();

		assertStatesContain(callback, PlaybackStatus.State.PRELOADED);
	}

	@Test
	void shouldInvokeCallbackWhenPreloading() throws InterruptedException {
		initContext(PRE_START_DATE);

		List<PlaybackStatus.State> states = Collections.synchronizedList(new ArrayList<>());
		CountDownLatch latch = new CountDownLatch(2);
		context.setOnStatusChangeCallback(status -> {
			states.add(status.getState());
			latch.countDown();
		});

		context.preload();

		assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
		assertThat(states).containsExactly(PlaybackStatus.State.PRELOADING, PlaybackStatus.State.PRELOADED);
	}

	@Test
	void shouldInvokeCallbackWhenStartAndPause() throws InterruptedException {
		initContext(null);

		CountDownLatch takeLatch = new CountDownLatch(1);
		injectMockDataProvider(takeLatch);

		Consumer<PlaybackStatus> callback = mockCallback();
		context.setOnStatusChangeCallback(callback);

		context.start();
		assertStatesContain(callback, PlaybackStatus.State.PLAYING);

		context.pause();
		assertStatesContain(callback, PlaybackStatus.State.PRELOADED);
	}

	@Test
	void shouldInvokeCallbackWhenSetSpeed() {
		initContext(null);

		Consumer<PlaybackStatus> callback = mockCallback();
		context.setOnStatusChangeCallback(callback);

		context.setSpeed(100);

		ArgumentCaptor<PlaybackStatus> captor = ArgumentCaptor.forClass(PlaybackStatus.class);
		verify(callback, atLeastOnce()).accept(captor.capture());
		assertThat(captor.getValue().getPlayInterval()).isEqualTo(100);
	}

	@Test
	void shouldInvokeCallbackWhenSetRunningTime() {
		initContext(null);

		Consumer<PlaybackStatus> callback = mockCallback();
		context.setOnStatusChangeCallback(callback);

		long timestamp = System.currentTimeMillis();
		context.setRunningTime(timestamp);

		ArgumentCaptor<PlaybackStatus> captor = ArgumentCaptor.forClass(PlaybackStatus.class);
		verify(callback, atLeastOnce()).accept(captor.capture());
		assertThat(captor.getValue().getRunningDateTime())
				.isEqualTo(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
	}

	@Test
	void shouldInvokeCallbackWhenInterruptedDuringPlaying() throws InterruptedException {
		initContext(null);

		CountDownLatch takeLatch = new CountDownLatch(1);
		injectMockDataProvider(takeLatch);

		CountDownLatch finishLatch = new CountDownLatch(1);
		Consumer<PlaybackStatus> callback = mockCallback();
		context.setOnStatusChangeCallback(status -> {
			callback.accept(status);
			if (status.getState() == PlaybackStatus.State.FINISHED) {
				finishLatch.countDown();
			}
		});

		context.start();

		// 等待回放线程真正进入阻塞读取状态
		assertThat(takeLatch.await(2, TimeUnit.SECONDS)).isTrue();

		// 中断回放线程，模拟回放结束
		ExecutorService exec = getExecutor(context);
		exec.shutdownNow();

		assertThat(finishLatch.await(2, TimeUnit.SECONDS)).isTrue();
		assertStatesContain(callback, PlaybackStatus.State.FINISHED);
	}

	private void initContext(String preStartDate) {
		PlaybackGatewaySettings settings = PlaybackGatewaySettings.builder()
				.preStartDate(preStartDate)
				.startDate(START_DATE)
				.endDate(END_DATE)
				.precision(PlaybackPrecision.LITE)
				.speed(PlaybackSpeed.RUSH)
				.build();

		GatewayDescription description = GatewayDescription.builder()
				.gatewayId("PLAYBACK")
				.subscribedContracts(Collections.emptyList())
				.settings(settings)
				.build();

		gateway = new PlaybackGatewayAdapter(description, feEngine, mktCenter);
		context = gateway.getContext();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void injectMockDataProvider(CountDownLatch takeLatch) {
		PlaybackDataProvider mockProvider = mock(PlaybackDataProvider.class);
		ArrayBlockingQueue<?> bufferMock = mock(ArrayBlockingQueue.class);
		try {
			when(bufferMock.take()).thenAnswer(invocation -> {
				takeLatch.countDown();
				// 模拟阻塞读取，直到被中断
				Thread.sleep(60_000);
				return null;
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		when(mockProvider.getBuffer()).thenReturn((ArrayBlockingQueue) bufferMock);
		setField(context, "dataProvider", mockProvider);
	}

	private Consumer<PlaybackStatus> mockCallback() {
		@SuppressWarnings("unchecked")
		Consumer<PlaybackStatus> callback = mock(Consumer.class);
		return callback;
	}

	private void assertStatesContain(Consumer<PlaybackStatus> callback, PlaybackStatus.State expected) {
		ArgumentCaptor<PlaybackStatus> captor = ArgumentCaptor.forClass(PlaybackStatus.class);
		verify(callback, atLeastOnce()).accept(captor.capture());
		assertThat(captor.getAllValues())
				.extracting(PlaybackStatus::getState)
				.contains(expected);
	}

	private void shutdownExecutor(PlaybackContext ctx) {
		ExecutorService exec = getExecutor(ctx);
		if (exec != null && !exec.isShutdown()) {
			exec.shutdownNow();
		}
	}

	private ExecutorService getExecutor(PlaybackContext ctx) {
		try {
			Field field = PlaybackContext.class.getDeclaredField("exec");
			field.setAccessible(true);
			return (ExecutorService) field.get(ctx);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void setField(Object target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
