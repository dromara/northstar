package org.dromara.northstar.gateway.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.PlaybackPrecision;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.gateway.IContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlaybackDataLoaderTest {
	
	IDataSource ds = mock(IDataSource.class);
	
	IContract ic = mock(IContract.class);
	
	Contract c = mock(Contract.class);
	
	PlaybackDataLoader loader = new PlaybackDataLoader("testGateway", List.of(ic), PlaybackPrecision.MEDIUM);
	
	Bar bar = Bar.builder()
			.actionDay(LocalDate.now())
			.actionTime(LocalTime.now().withSecond(0).withNano(0))
			.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.now().withSecond(0).withNano(0)))
			.contract(c)
			.openPrice(1000)
			.closePrice(1050)
			.highPrice(1100)
			.lowPrice(1000)
			.build();
	
	ContractDefinition cd = ContractDefinition.builder()
			.tradeTimeDef(TradeTimeDefinition.builder()
					.timeSlots(List.of(TimeSlot.builder()
							.start(LocalTime.of(0, 0))
							.end(LocalTime.of(23, 59))
							.build()))
					.build())
			.build();
	
	@BeforeEach
	void prepare() {
		when(ic.dataSource()).thenReturn(ds);
		when(ic.contract()).thenReturn(c);
		when(c.contractDefinition()).thenReturn(cd);
		when(ds.getMinutelyData(any(), any(), any())).thenReturn(List.of(bar));
	}

	@Test
	void testPreload() throws InterruptedException, ExecutionException {
		AtomicInteger cnt = new AtomicInteger();
		CompletableFuture<Void> cf = loader.preload(LocalDate.now().minusDays(1), LocalDate.now(), df -> {
			try {
				Thread.sleep(500);
				assertThat(df.items()).isNotEmpty();
				assertThat(df.getTimestamp()).isEqualTo(bar.actionTimestamp());
				assertThat(cnt.incrementAndGet()).isEqualTo(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		assertThat(cnt.incrementAndGet()).isEqualTo(1);
		cf.get();
		assertThat(cnt.incrementAndGet()).isEqualTo(3);
	}

	@SuppressWarnings("unchecked")
	@Test
	void testLoad() throws InterruptedException, ExecutionException {
		AtomicInteger cnt = new AtomicInteger();
		Consumer<Object> dummyObj = mock(Consumer.class);
		Consumer<Object> dummyObj2 = mock(Consumer.class);
		CompletableFuture<Void> cf = loader.load(LocalDate.now().minusDays(1), LocalDate.now(), () -> false, 
				tdf -> {
					assertThat(cnt.getAndIncrement()).isLessThanOrEqualTo(30);
					dummyObj.accept(cnt);
				},
				(dfList, flag) -> {
					assertThat(cnt.getAndIncrement()).isEqualTo(31);
					dummyObj2.accept(cnt);
				});
		assertThat(cnt.getAndIncrement()).isEqualTo(0);
		cf.get();
		
		verify(dummyObj, times(30)).accept(any());
		verify(dummyObj2, times(1)).accept(any());
	}

}
