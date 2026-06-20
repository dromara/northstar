package org.dromara.northstar.gateway.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.event.FastEventEngine;
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
import org.mockito.Mockito;

/**
 * PlaybackDataProvider 单元测试
 * 
 * 重点验证 setRunningTime 后：
 * 1. 可以清空 dataBuffer；
 * 2. 再次 startSupply 时，加载线程按新的 dataLoadCursor 重新加载，
 *    不会有旧数据残留（包括缓冲区已满、offer 循环空转的并发场景）。
 */
class PlaybackDataProviderTest {

	private static final String START_DATE = "20240102";
	private static final String END_DATE = "20240103";
	private static final String PRE_START_DATE = "20240101";
	private static final String CONTRACT_ID = "TEST@SHFE@FUTURES@PLAYBACK";

	private FastEventEngine feEngine;
	private MarketCenter mktCenter;
	private IContract mockIContract;
	private Contract testContract;
	private PlaybackContext context;
	private PlaybackDataProvider provider;
	private PlaybackDataLoader mockLoader;

	@BeforeEach
	void setUp() {
		feEngine = mock(FastEventEngine.class);
		mktCenter = mock(MarketCenter.class);
		mockIContract = mock(IContract.class);
		testContract = makeTestContract();
		when(mockIContract.contract()).thenReturn(testContract);
		when(mktCenter.getContract(any(Identifier.class))).thenReturn(mockIContract);

		PlaybackGatewaySettings settings = PlaybackGatewaySettings.builder()
				.preStartDate(PRE_START_DATE)
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

		PlaybackGatewayAdapter gateway = new PlaybackGatewayAdapter(description, feEngine, mktCenter);
		context = gateway.getContext();
		provider = new PlaybackDataProvider(context);

		mockLoader = mock(PlaybackDataLoader.class);
		setField(provider, "dataLoader", mockLoader);
	}

	@AfterEach
	void tearDown() {
		provider.pauseSupply();
		shutdownExecutor(provider);
		if (context != null) {
			shutdownExecutor(context);
		}
	}

	@Test
	void setRunningTimeShouldClearDataBuffer() throws InterruptedException {
		LocalDateTime newCursor = LocalDateTime.of(2024, 1, 1, 0, 0);

		// 模拟缓冲区中已有的数据
		LocalDateTime dayStart = LocalDateTime.of(2024, 1, 2, 0, 0);
		provider.getBuffer().put(makeBarFrame(dayStart, dayStart.plusMinutes(1)));
		provider.getBuffer().put(makeBarFrame(dayStart.plusMinutes(10), dayStart.plusMinutes(11)));

		assertThat(provider.getBuffer()).hasSize(2);

		provider.setRunningTime(newCursor);

		assertThat(provider.getBuffer()).isEmpty();
	}

	@Test
	void setRunningTimeDuringHighVolumeOfferLoopShouldReloadWithoutResidual() throws Exception {
		LocalDateTime oldCursor = LocalDateTime.of(2024, 1, 2, 0, 0);
		LocalDateTime newCursor = LocalDateTime.of(2024, 1, 1, 0, 0);

		// 第一次加载返回超过 5000 帧，超过缓冲区容量（4096），让加载线程卡在 offer 循环中
		List<DataFrame<Bar>> hugeData = new ArrayList<>();
		for (int i = 0; i < 5001; i++) {
			hugeData.add(makeBarFrame(oldCursor.plusMinutes(i), oldCursor.plusMinutes(i + 1)));
		}
		when(mockLoader.loadData(anyLong(), anyLong())).thenReturn(hugeData);

		provider.startSupply();

		// 等待缓冲区被填满，确认加载线程正在 offer 循环中空转
		long deadline = System.currentTimeMillis() + 2000;
		while (provider.getBuffer().size() < 4096 && System.currentTimeMillis() < deadline) {
			Thread.sleep(10);
		}
		assertThat(provider.getBuffer().size()).isEqualTo(4096);

		// 在 offer 循环空转时替换 loader：setRunningTime 后只允许返回新光标的数据
		Mockito.reset(mockLoader);
		CountDownLatch loadLatch = new CountDownLatch(1);
		AtomicReference<Long> capturedStart = new AtomicReference<>();
		long newCursorMillis = CommonUtils.localDateTimeToMills(newCursor);
		Mockito.doAnswer(invocation -> {
			long startTime = invocation.getArgument(0);
			if (capturedStart.get() == null) {
				capturedStart.set(startTime);
				loadLatch.countDown();
			}
			if (startTime == newCursorMillis) {
				return List.of(makeBarFrame(newCursor, newCursor.plusMinutes(1)));
			}
			return Collections.emptyList();
		}).when(mockLoader).loadData(anyLong(), anyLong());

		// 在 offer 循环空转时执行 setRunningTime：旧线程应终止并清空缓冲区
		provider.setRunningTime(newCursor);
		assertThat(provider.getBuffer()).isEmpty();

		// 重新启动供应，验证按新光标加载
		provider.startSupply();

		assertThat(loadLatch.await(2, TimeUnit.SECONDS)).isTrue();
		assertThat(capturedStart.get()).isEqualTo(newCursorMillis);

		DataFrame<Bar> newFrame = provider.getBuffer().poll(2, TimeUnit.SECONDS);
		assertThat(newFrame).isNotNull();
		assertThat(newFrame.getTime().getStart()).isEqualTo(newCursor);

		// 确保没有之前的大批量旧数据残留
		assertThat(provider.getBuffer()).isEmpty();
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

	private void shutdownExecutor(Object target) {
		ExecutorService exec = getExecutor(target);
		if (exec != null && !exec.isShutdown()) {
			exec.shutdownNow();
		}
	}

	private ExecutorService getExecutor(Object target) {
		try {
			Field field = target.getClass().getDeclaredField("exec");
			field.setAccessible(true);
			return (ExecutorService) field.get(target);
		} catch (NoSuchFieldException e) {
			return null;
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
