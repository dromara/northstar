package org.dromara.northstar.support.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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

	@Mock
	private IContract icontract;
	
	@Mock
	private IDataSource dataSource;
	
	@Mock
	private KlineGenerator klineGenerator;
	
	@Test
	void shouldLoadBarsInChronologicalOrderWhenLoadingThreeWeeksConcurrently() {
		when(icontract.dataSource()).thenReturn(dataSource);
		when(icontract.contract()).thenReturn(contract());
		when(icontract.name()).thenReturn("TEST");
		when(dataSource.getData(any(Contract.class), eq(Interval.M1), any(LocalDate.class), any(LocalDate.class)))
			.thenAnswer(invocation -> {
				LocalDate start = invocation.getArgument(2);
				LocalDate end = invocation.getArgument(3);
				// 模拟不同周请求完成顺序不确定
				Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50));
				return makeBars(start, end, true);
			});
		
		ModuleDataLoader loader = new ModuleDataLoader(icontract, klineGenerator);
		loader.load(3);
		
		// 验证按 3 个周窗口发起了请求
		verify(dataSource, times(3)).getData(any(Contract.class), eq(Interval.M1), any(LocalDate.class), any(LocalDate.class));
		
		ArgumentCaptor<Bar> captor = ArgumentCaptor.forClass(Bar.class);
		verify(klineGenerator, times(21)).updateBar(captor.capture());
		
		List<Long> timestamps = captor.getAllValues().stream()
				.map(Bar::actionTimestamp)
				.toList();
		assertThat(timestamps).isSorted();
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
		when(dataSource.getData(any(Contract.class), eq(Interval.M1), any(LocalDate.class), any(LocalDate.class)))
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
		LocalDate today = LocalDate.now();
		when(dataSource.getData(any(Contract.class), eq(Interval.M1), any(LocalDate.class), any(LocalDate.class)))
			.thenReturn(List.of(
				makeBar(today, 4L),
				makeBar(today.minusDays(1), 3L),
				makeBar(today.minusDays(2), 2L),
				makeBar(today.minusDays(3), 1L)
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
	
	private Bar makeBar(LocalDate date, long actionTimestamp) {
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
	
	private List<Bar> makeBars(LocalDate startDate, LocalDate endDate, boolean newestFirst) {
		List<Bar> bars = new ArrayList<>();
		for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
			long timestamp = date.toEpochDay() * 86_400_000L + 60_000L;
			bars.add(makeBar(date, timestamp));
		}
		if (newestFirst) {
			bars = bars.reversed();
		}
		return bars;
	}
	
}
