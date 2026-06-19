package org.dromara.northstar.gateway.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.exception.OperationException;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.gateway.playback.model.DataFrame;
import org.dromara.northstar.gateway.playback.model.PlaybackPrecision;
import org.dromara.northstar.gateway.playback.model.PlaybackSpeed;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * PlaybackContext 状态流转与生命周期测试
 * 
 * 本测试覆盖 PlaybackContext 的核心场景：
 * - 重置（reset）后回到 INITIAL 或 PRELOADED
 * - 预热（preload）结束并进入 PRELOADED
 * - 正常开始回放（start）并进入 PLAYING
 * - 暂停（pause）后回到 PRELOADED
 * - 正常结束回放到 FINISHED
 * - 参数设置（speed、runningTime）的状态通知
 * 
 * 为了避免真实数据加载带来的不稳定与耗时，测试中通过反射注入 mock 的 PlaybackDataProvider，
 * 使用可控的 DataFrame<Bar> 驱动状态机流转。
 */
class PlaybackContextTest {

	private static final String START_DATE = "20240102";
	private static final String END_DATE = "20240103";
	private static final String PRE_START_DATE = "20240101";
	private static final String CONTRACT_ID = "TEST@SHFE@FUTURES@PLAYBACK";

	private FastEventEngine feEngine;
	private MarketCenter mktCenter;
	private IContract mockIContract;
	private Contract testContract;
	private PlaybackGatewayAdapter gateway;
	private PlaybackContext context;

	@BeforeEach
	void setUp() {
		feEngine = mock(FastEventEngine.class);
		mktCenter = mock(MarketCenter.class);
		mockIContract = mock(IContract.class);
		testContract = makeTestContract();
		when(mockIContract.contract()).thenReturn(testContract);
		when(mktCenter.getContract(any(Identifier.class))).thenReturn(mockIContract);
	}

	@AfterEach
	void tearDown() {
		// 确保测试过程中创建的线程池都被关闭，避免测试用例之间互相影响及线程泄漏
		if (context != null) {
			shutdownExecutor(context);
		}
	}

	@Test
	void shouldResetToInitialWhenPreloadDateBeforeStartDate() {
		initContext(PRE_START_DATE);

		Consumer<PlaybackStatus> callback = mockCallback();
		context.setOnStatusChangeCallback(callback);

		context.reset();

		assertThat(context.getPlaybackStatus().getState()).isEqualTo(PlaybackStatus.State.INITIAL);
		assertThat(context.getPlaybackStatus().getRunningDateTime())
				.isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0));
		assertStatesContain(callback, PlaybackStatus.State.INITIAL);
	}

	@Test
	void shouldResetToPreloadedWhenPreloadDateEqualsStartDate() {
		initContext(null);

		Consumer<PlaybackStatus> callback = mockCallback();
		context.setOnStatusChangeCallback(callback);

		context.reset();

		assertThat(context.getPlaybackStatus().getState()).isEqualTo(PlaybackStatus.State.PRELOADED);
		assertThat(context.getPlaybackStatus().getRunningDateTime())
				.isEqualTo(LocalDateTime.of(2024, 1, 2, 0, 0));
		assertStatesContain(callback, PlaybackStatus.State.PRELOADED);
	}

	@Test
	void shouldFinishPreloadingAndReachPreloadedState() throws InterruptedException {
		initContext(PRE_START_DATE);

		// 预热结束时间点为 2024-01-02 00:00，提供一个刚好跨越该时间点的数据帧
		LocalDateTime preloadEnd = LocalDateTime.of(2024, 1, 2, 0, 0);
		DataFrame<Bar> preloadFrame = makeBarFrame(preloadEnd.minusMinutes(1), preloadEnd);
		injectMockProviderForPreload(preloadFrame);

		List<PlaybackStatus.State> states = Collections.synchronizedList(new ArrayList<>());
		CountDownLatch latch = new CountDownLatch(2);
		context.setOnStatusChangeCallback(status -> {
			// runningDateTime 更新也会触发回调，这里只关注状态变化
			PlaybackStatus.State state = status.getState();
			if (states.isEmpty() || states.get(states.size() - 1) != state) {
				states.add(state);
				latch.countDown();
			}
		});

		context.preload();

		assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
		assertThat(states).containsExactly(PlaybackStatus.State.PRELOADING, PlaybackStatus.State.PRELOADED);
		assertThat(context.getPlaybackStatus().getState()).isEqualTo(PlaybackStatus.State.PRELOADED);
		assertThat(context.getPlaybackStatus().getRunningDateTime()).isEqualTo(preloadEnd);
		assertThat(gateway.getConnection()).isEqualTo(ConnectionState.DISCONNECTED);
	}

	@Test
	void shouldStartPlayingAndThenPause() throws InterruptedException {
		initContext(null);

		CountDownLatch takeLatch = new CountDownLatch(1);
		injectBlockingProvider(takeLatch);

		Consumer<PlaybackStatus> callback = mockCallback();
		context.setOnStatusChangeCallback(callback);

		context.start();
		assertThat(context.getPlaybackStatus().getState()).isEqualTo(PlaybackStatus.State.PLAYING);
		assertThat(gateway.getConnection()).isEqualTo(ConnectionState.CONNECTED);
		assertStatesContain(callback, PlaybackStatus.State.PLAYING);

		// 等待回放线程真正进入 take 阻塞，确保 pause 作用于运行中的回放
		assertThat(takeLatch.await(2, TimeUnit.SECONDS)).isTrue();
		context.pause();

		assertThat(context.getPlaybackStatus().getState()).isEqualTo(PlaybackStatus.State.PRELOADED);
		assertThat(gateway.getConnection()).isEqualTo(ConnectionState.DISCONNECTED);
		assertStatesContain(callback, PlaybackStatus.State.PRELOADED);
	}

	@Test
	void shouldFinishPlayingNormally() throws InterruptedException {
		initContext(null);

		CountDownLatch finishLatch = new CountDownLatch(1);
		Consumer<PlaybackStatus> callback = mockCallback();
		context.setOnStatusChangeCallback(status -> {
			callback.accept(status);
			if (status.getState() == PlaybackStatus.State.FINISHED) {
				finishLatch.countDown();
			}
		});

		// 提供一个结束时间刚好等于 endDate + 1 天的数据帧，让回放线程自然退出
		LocalDateTime endDateTime = LocalDate.parse(END_DATE, DateTimeConstant.D_FORMAT_INT_FORMATTER).plusDays(1).atStartOfDay();
		DataFrame<Bar> endFrame = makeBarFrame(endDateTime.minusMinutes(1), endDateTime);
		injectMockProviderForPlayback(List.of(endFrame));

		context.start();

		assertThat(finishLatch.await(3, TimeUnit.SECONDS)).isTrue();
		assertThat(context.getPlaybackStatus().getState()).isEqualTo(PlaybackStatus.State.FINISHED);
		assertThat(gateway.getConnection()).isEqualTo(ConnectionState.DISCONNECTED);
		assertStatesContain(callback, PlaybackStatus.State.FINISHED);
	}

	@Test
	void shouldNotStartWhenInInitialState() {
		initContext(PRE_START_DATE);

		assertThatThrownBy(() -> context.start())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("需先进行数据预加载");
	}

	@Test
	void shouldNotPreloadWhenNotInInitialState() {
		initContext(null);

		// OperationException 在测试环境未初始化 MessageSource 时会转为 IllegalStateException
		assertThatThrownBy(() -> context.preload())
				.isInstanceOfAny(OperationException.class, IllegalStateException.class);
	}

	@Test
	void shouldSuspendAndResumeWithoutChangingState() throws InterruptedException {
		initContext(null);

		CountDownLatch takeLatch = new CountDownLatch(1);
		injectBlockingProvider(takeLatch);

		context.start();
		assertThat(takeLatch.await(2, TimeUnit.SECONDS)).isTrue();

		context.suspend();
		assertThat(context.getPlaybackStatus().getState()).isEqualTo(PlaybackStatus.State.PLAYING);

		context.resume();
		assertThat(context.getPlaybackStatus().getState()).isEqualTo(PlaybackStatus.State.PLAYING);
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
		injectBlockingProvider(takeLatch);

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

		// 中断回放线程，模拟外部强制结束
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
				.subscribedContracts(List.of(ContractSimpleInfo.builder()
						.id(CONTRACT_ID)
						.name("TEST")
						.unifiedSymbol("TEST@SHFE@FUTURES")
						.channelType(ChannelType.PLAYBACK)
						.build()))
				.settings(settings)
				.build();

		gateway = new PlaybackGatewayAdapter(description, feEngine, mktCenter);
		context = gateway.getContext();
	}

	private Contract makeTestContract() {
		ContractDefinition cd = ContractDefinition.builder()
				.tradeTimeDef(TradeTimeDefinition.builder()
						.timeSlots(List.of(TimeSlot.builder()
								.start(LocalTime.of(0, 0))
								.end(LocalTime.of(23, 59))
								.build()))
						.build())
				.build();
		return Contract.builder()
				.gatewayId("PLAYBACK")
				.contractId(CONTRACT_ID)
				.name("TEST")
				.unifiedSymbol("TEST@SHFE@FUTURES")
				.symbol("TEST")
				.priceTick(1)
				.channelType(ChannelType.PLAYBACK)
				.usingStartTimeInBarTime(false)
				.contractDefinition(cd)
				.build();
	}

	private Bar makeBar(LocalDateTime ldt) {
		return Bar.builder()
				.gatewayId("PLAYBACK")
				.contract(testContract)
				.actionDay(ldt.toLocalDate())
				.actionTime(ldt.toLocalTime())
				.tradingDay(ldt.toLocalDate())
				.actionTimestamp(CommonUtils.localDateTimeToMills(ldt))
				.openPrice(5000)
				.highPrice(5001)
				.lowPrice(4999)
				.closePrice(5000)
				.openInterest(100)
				.openInterestDelta(1)
				.volume(1000)
				.volumeDelta(10)
				.turnover(500000)
				.turnoverDelta(5000)
				.preOpenInterest(99)
				.preClosePrice(4999)
				.preSettlePrice(4999)
				.interval(Interval.M1)
				.unstable(false)
				.channelType(ChannelType.PLAYBACK)
				.build();
	}

	private DataFrame<Bar> makeBarFrame(LocalDateTime start, LocalDateTime end) {
		DataFrame<Bar> df = new DataFrame<>(start, end);
		df.add(makeBar(start));
		return df;
	}

	private void injectMockProviderForPreload(DataFrame<Bar> preloadFrame) throws InterruptedException {
		PlaybackDataProvider mockProvider = mock(PlaybackDataProvider.class);
		ArrayBlockingQueue<DataFrame<Bar>> preloadBuffer = new ArrayBlockingQueue<>(10);
		if (preloadFrame != null) {
			preloadBuffer.put(preloadFrame);
		}
		when(mockProvider.loadPreloadData()).thenReturn(preloadBuffer);
		setField(context, "dataProvider", mockProvider);
	}

	private void injectMockProviderForPlayback(List<DataFrame<Bar>> frames) throws InterruptedException {
		PlaybackDataProvider mockProvider = mock(PlaybackDataProvider.class);
		ArrayBlockingQueue<DataFrame<Bar>> buffer = new ArrayBlockingQueue<>(10);
		for (DataFrame<Bar> frame : frames) {
			buffer.put(frame);
		}
		when(mockProvider.getBuffer()).thenReturn(buffer);
		setField(context, "dataProvider", mockProvider);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void injectBlockingProvider(CountDownLatch takeLatch) {
		PlaybackDataProvider mockProvider = mock(PlaybackDataProvider.class);
		ArrayBlockingQueue<?> bufferMock = mock(ArrayBlockingQueue.class);
		CountDownLatch blockLatch = new CountDownLatch(1);
		try {
			when(bufferMock.take()).thenAnswer(invocation -> {
				takeLatch.countDown();
				// 模拟阻塞读取，直到被中断
				blockLatch.await();
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
