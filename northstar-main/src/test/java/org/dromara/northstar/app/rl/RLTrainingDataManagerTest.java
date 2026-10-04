package org.dromara.northstar.app.rl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.gateway.IContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RLTrainingDataManagerTest {

	private static final String SYMBOL = "rb0000@SHFE@FUTURES";
	private static final String TASK_ID = "task-1";

	@TempDir
	Path dataRoot;

	RLTrainingDataManager manager;
	Contract contract;
	IContract ic;
	IDataSource dataSource;

	@BeforeEach
	void setUp() {
		manager = new RLTrainingDataManager(dataRoot);
		contract = Contract.builder()
				.contractId(SYMBOL + "@INDEX")
				.unifiedSymbol(SYMBOL)
				.symbol("rb0000")
				.name("螺纹钢指数")
				.exchange(xyz.redtorch.pb.CoreEnum.ExchangeEnum.SHFE)
				.productClass(xyz.redtorch.pb.CoreEnum.ProductClassEnum.FUTURES)
				.multiplier(10)
				.priceTick(1.0)
				.channelType(ChannelType.PLAYBACK)
				.build();
		dataSource = mock(IDataSource.class);
		ic = mock(IContract.class);
		when(ic.contract()).thenReturn(contract);
		when(ic.dataSource()).thenReturn(dataSource);
	}

	/** 数据源只在首个分页周内有数据（降序返回），之后连续空页触发翻页终止 */
	private void stubOneWeekOfBars(LocalDate startDate) {
		LocalDate dataEnd = startDate.plusDays(4);
		when(dataSource.getData(eq(contract), eq(Interval.M1), any(LocalDate.class), any(LocalDate.class)))
				.thenAnswer(inv -> {
					LocalDate from = inv.getArgument(2);
					LocalDate to = inv.getArgument(3);
					List<Bar> bars = new ArrayList<>();
					for (LocalDate day = from; !day.isAfter(to) && !day.isAfter(dataEnd); day = day.plusDays(1)) {
						if (day.isBefore(startDate)) {
							continue;
						}
						for (int i = 0; i < 3; i++) {
							LocalTime time = LocalTime.of(10, 1).plusMinutes(i);
							bars.add(Bar.builder()
									.contract(contract)
									.actionDay(day)
									.actionTime(time)
									.tradingDay(day)
									.actionTimestamp(day.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
									.openPrice(3000 + i).highPrice(3010 + i)
									.lowPrice(2990 + i).closePrice(3005 + i)
									.openInterest(5000 + i).openInterestDelta(i)
									.volume(1000 + i).volumeDelta(10 + i)
									.turnover(1e6 + i).turnoverDelta(1e4 + i)
									.preOpenInterest(4990 + i).preClosePrice(2999 + i).preSettlePrice(2998 + i)
									.interval(Interval.M1)
									.channelType(ChannelType.PLAYBACK)
									.build());
						}
					}
					bars.sort(Comparator.comparingLong(Bar::actionTimestamp).reversed());	// 数据源返回降序
					return bars;
				});
	}

	@Test
	void fetchSaveLoadRoundTrip() throws IOException {
		LocalDate startDate = LocalDate.now().minusDays(30);
		stubOneWeekOfBars(startDate);

		int written = manager.fetchAndSave(ic, startDate, TASK_ID);

		assertEquals(15, written, "5 天 × 3 根 bar");
		assertTrue(manager.dataExists(TASK_ID, SYMBOL), "落盘后应判定数据存在");
		assertFalse(Files.exists(dataRoot.resolve(TASK_ID).resolve(SYMBOL + ".tmp")), "临时文件应已原子改名");

		List<Bar> bars = manager.load(TASK_ID, contract);
		assertEquals(written, bars.size());
		assertTrue(bars.stream().allMatch(b -> b.contract().equals(contract)));
		assertTrue(bars.stream().allMatch(b -> b.interval() == Interval.M1));
		assertTrue(bars.stream().allMatch(b -> b.channelType() == ChannelType.PLAYBACK));
		for (int i = 1; i < bars.size(); i++) {
			assertTrue(bars.get(i).actionTimestamp() >= bars.get(i - 1).actionTimestamp(), "读回数据应为时间升序");
		}
		Bar first = bars.get(0);
		assertEquals(startDate, first.actionDay());
		assertEquals(LocalTime.of(10, 1), first.actionTime());
		assertEquals(3000, first.openPrice());
		assertEquals(3010, first.highPrice());
		assertEquals(2990, first.lowPrice());
		assertEquals(3005, first.closePrice());
		assertEquals(5000, first.openInterest());
		assertEquals(1000, first.volume());
		assertEquals(10, first.volumeDelta());
		assertEquals(1e6, first.turnover());
		assertEquals(1e4, first.turnoverDelta());
		assertEquals(4990, first.preOpenInterest());
		assertEquals(2999, first.preClosePrice());
		assertEquals(2998, first.preSettlePrice());
	}

	@Test
	void dataExistsIgnoresTmpFile() throws IOException {
		Path taskDir = dataRoot.resolve(TASK_ID);
		Files.createDirectories(taskDir);
		Files.writeString(taskDir.resolve(SYMBOL + ".tmp"), "半截文件");

		assertFalse(manager.dataExists(TASK_ID, SYMBOL), ".tmp 半截文件不算已加载");
	}

	@Test
	void loadThrowsWhenFileMissing() {
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> manager.load(TASK_ID, contract));
		assertTrue(e.getMessage().contains("数据文件不存在"));
	}
}
