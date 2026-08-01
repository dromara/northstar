package org.dromara.northstar.support.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.kline.KlineGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

@ExtendWith(MockitoExtension.class)
class ModuleDataLoaderTest {

	private static final long WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000;

	@Mock
	private IContract icontract;
	
	@Mock
	private IDataSource dataSource;
	
	@Mock
	private KlineGenerator klineGenerator;
	
	@Test
	void shouldLoadBarsInChronologicalOrderWhenLoadingThreeWeeks() {
		when(icontract.dataSource()).thenReturn(dataSource);
		when(icontract.contract()).thenReturn(contract());
		when(icontract.name()).thenReturn("TEST");
		when(dataSource.getData(any(Contract.class), eq(Interval.M1), anyLong(), anyLong()))
			.thenAnswer(invocation -> {
				long start = invocation.getArgument(2);
				long end = invocation.getArgument(3);
				// 模拟不同周请求耗时不确定
				Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50));
				return makeBars(start, end, true);
			});
		
		ModuleDataLoader loader = new ModuleDataLoader(icontract, klineGenerator);
		loader.load(3);
		
		// 验证按 3 个周窗口发起了请求
		verify(dataSource, times(3)).getData(any(Contract.class), eq(Interval.M1), anyLong(), anyLong());
		
		ArgumentCaptor<Bar> captor = ArgumentCaptor.forClass(Bar.class);
		// 最后一个窗口的结束时间会延伸到请求发起时的当前时间，因此数据条数不少于 21
		verify(klineGenerator, atLeast(21)).updateBar(captor.capture());
		
		List<Long> timestamps = captor.getAllValues().stream()
				.map(Bar::actionTimestamp)
				.toList();
		assertThat(timestamps).isSorted();
	}
	
	@Test
	void shouldLoadDataPreciselyUpToCurrentTime() {
		when(icontract.dataSource()).thenReturn(dataSource);
		when(icontract.contract()).thenReturn(contract());
		when(icontract.name()).thenReturn("TEST");
		when(dataSource.getData(any(Contract.class), eq(Interval.M1), anyLong(), anyLong()))
			.thenReturn(List.of());
		
		long before = System.currentTimeMillis();
		new ModuleDataLoader(icontract, klineGenerator).load(3);
		long after = System.currentTimeMillis();
		
		ArgumentCaptor<Long> startCaptor = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<Long> endCaptor = ArgumentCaptor.forClass(Long.class);
		verify(dataSource, times(3)).getData(any(Contract.class), eq(Interval.M1), startCaptor.capture(), endCaptor.capture());
		
		// 预取是并发进行的，调用顺序不确定，需先排序再断言
		List<Long> starts = new ArrayList<>(startCaptor.getAllValues());
		List<Long> ends = new ArrayList<>(endCaptor.getAllValues());
		Collections.sort(starts);
		Collections.sort(ends);
		// 各窗口应首尾相接：除整体起点与终点外，边界一一对应
		assertThat(starts.subList(1, starts.size())).isEqualTo(ends.subList(0, ends.size() - 1));
		// 起点精确为当前时间往前推 3 周
		assertThat(starts.get(0)).isBetween(before - 3 * WEEK_MILLIS, after - 3 * WEEK_MILLIS);
		// 终点为最后一个窗口请求发起时的当前时间，不早于加载开始时间
		assertThat(ends.get(ends.size() - 1)).isBetween(before, after);
	}
	
	@Test
	void shouldDoNothingWhenWeeksIsZeroOrNegative() {
		ModuleDataLoader loader = new ModuleDataLoader(icontract, klineGenerator);
		
		loader.load(0);
		loader.load(-1);
		
		verifyNoInteractions(klineGenerator, icontract, dataSource);
	}
	
	@Test
	void shouldHandleEmptyDataSource() {
		when(icontract.dataSource()).thenReturn(dataSource);
		when(icontract.contract()).thenReturn(contract());
		when(icontract.name()).thenReturn("TEST");
		when(dataSource.getData(any(Contract.class), eq(Interval.M1), anyLong(), anyLong()))
			.thenReturn(List.of());
		
		ModuleDataLoader loader = new ModuleDataLoader(icontract, klineGenerator);
		loader.load(1);
		
		verify(klineGenerator, never()).updateBar(any());
	}
	
	@Test
	void shouldThrowWhenDataSourceIsMissing() {
		ModuleDataLoader loader = new ModuleDataLoader(icontract, klineGenerator);
		
		assertThatThrownBy(() -> loader.load(1))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("缺少数据源");
	}
	
	@Test
	void shouldLoadBarsInChronologicalOrderWhenSingleWeekDataIsNewestFirst() {
		when(icontract.dataSource()).thenReturn(dataSource);
		when(icontract.contract()).thenReturn(contract());
		when(icontract.name()).thenReturn("TEST");
		
		// 模拟真实数据源：返回的数据按时间倒序（最新在前）
		when(dataSource.getData(any(Contract.class), eq(Interval.M1), anyLong(), anyLong()))
			.thenReturn(List.of(
				makeBar(4L),
				makeBar(3L),
				makeBar(2L),
				makeBar(1L)
			));
		
		ModuleDataLoader loader = new ModuleDataLoader(icontract, klineGenerator);
		loader.load(1);
		
		ArgumentCaptor<Bar> captor = ArgumentCaptor.forClass(Bar.class);
		verify(klineGenerator, times(4)).updateBar(captor.capture());
		
		List<Long> timestamps = captor.getAllValues().stream()
				.map(Bar::actionTimestamp)
				.toList();
		assertThat(timestamps).containsExactly(1L, 2L, 3L, 4L);
	}
	
	private Contract contract() {
		return Contract.builder()
				.gatewayId("GW")
				.contractId("TEST@SHFE@FUTURES@GW")
				.unifiedSymbol("TEST@SHFE@FUTURES")
				.symbol("TEST")
				.name("TEST")
				.exchange(ExchangeEnum.UnknownExchange)
				.productClass(ProductClassEnum.UnknownProductClass)
				.channelType(ChannelType.SIM)
				.build();
	}
	
	private Bar makeBar(long actionTimestamp) {
		LocalDate date = Instant.ofEpochMilli(actionTimestamp).atZone(ZoneId.systemDefault()).toLocalDate();
		return Bar.builder()
				.gatewayId("GW")
				.contract(contract())
				.actionDay(date)
				.tradingDay(date)
				.actionTime(LocalTime.of(9, 30))
				.actionTimestamp(actionTimestamp)
				.interval(Interval.M1)
				.channelType(ChannelType.SIM)
				.build();
	}
	
	private List<Bar> makeBars(long startTime, long endTime, boolean newestFirst) {
		List<Bar> bars = new ArrayList<>();
		for (long t = startTime; t < endTime; t += 86_400_000L) {
			bars.add(makeBar(t + 60_000L));
		}
		if (newestFirst) {
			bars = bars.reversed();
		}
		return bars;
	}
	
}
