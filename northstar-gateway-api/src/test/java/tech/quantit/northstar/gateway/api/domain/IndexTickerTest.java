package tech.quantit.northstar.gateway.api.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.TickField;

class IndexTickerTest {

	private TestFieldFactory factory = new TestFieldFactory("gateway");
	IndexContract idxContract = mock(IndexContract.class);
	
	@BeforeEach
	void prepare() {
		when(idxContract.unifiedSymbol()).thenReturn("rb0000@SHFE@FUTURES");
		when(idxContract.contractField()).thenReturn(factory.makeContract("rb0000"));
		when(idxContract.monthlyContractSymbols()).thenReturn(Set.of("rb2210@SHFE@FUTURES", "rb2201@SHFE@FUTURES", "rb2205@SHFE@FUTURES"));
	}
	
	@Test
	void test() {
		IndexTicker ticker = new IndexTicker(idxContract);
		Consumer<TickField> onTickCallback = mock(Consumer.class);
		ticker.setOnTickCallback(onTickCallback);
		LocalTime now = LocalTime.now();
		LocalTime later = now.plusSeconds(1);
		TickField t1 = randomTick("rb2210@SHFE@FUTURES", now);
		TickField t2 = randomTick("rb2201@SHFE@FUTURES", now);
		TickField t3 = randomTick("rb2205@SHFE@FUTURES", now);
		TickField t4 = randomTick("rb2210@SHFE@FUTURES", later);
		
		ticker.update(t1);
		ticker.update(t2);
		ticker.update(t3);
		ticker.update(t4);
		
		verify(onTickCallback).accept(any());
		assertThat(ticker.tickBuilder.getLastPrice()).isCloseTo((t1.getOpenInterest() * t1.getLastPrice() + t2.getOpenInterest() * t2.getLastPrice() + t3.getOpenInterest() * t3.getLastPrice()) / (t1.getOpenInterest() + t2.getOpenInterest() + t3.getOpenInterest()), offset(1D));
		assertThat(ticker.tickBuilder.getOpenInterest()).isCloseTo(t1.getOpenInterest() + t2.getOpenInterest() + t3.getOpenInterest(), offset(1e-6));
		assertThat(ticker.tickBuilder.getOpenInterestDelta()).isCloseTo(t1.getOpenInterestDelta() + t2.getOpenInterestDelta() + t3.getOpenInterestDelta(), offset(1e-6));
		assertThat(ticker.tickBuilder.getVolume()).isEqualTo(t1.getVolume() + t2.getVolume() + t3.getVolume());
		assertThat(ticker.tickBuilder.getVolumeDelta()).isEqualTo(t1.getVolumeDelta() + t2.getVolumeDelta() + t3.getVolumeDelta());
		assertThat(ticker.tickBuilder.getTurnover()).isCloseTo(t1.getTurnover() + t2.getTurnover() + t3.getTurnover(), offset(1e-6));
		assertThat(ticker.tickBuilder.getTurnoverDelta()).isCloseTo(t1.getTurnoverDelta() + t2.getTurnoverDelta() + t3.getTurnoverDelta(), offset(1e-6));
	}

	
	private TickField randomTick(String unifiedSymbol, LocalTime time) {
		return TickField.newBuilder()
				.setUnifiedSymbol(unifiedSymbol)
				.setTradingDay("20211215")
				.setActionDay("20211215")
				.setActionTime(time.format(DateTimeConstant.T_FORMAT_INT_FORMATTER))
				.setActionTimestamp(LocalDateTime.of(LocalDate.now(), time).toInstant(ZoneOffset.of("+8")).toEpochMilli())
				.setOpenInterest(ThreadLocalRandom.current().nextDouble(1000000000))
				.setOpenInterestDelta(ThreadLocalRandom.current().nextDouble(5000))
				.setLastPrice(ThreadLocalRandom.current().nextDouble(4000))
				.setVolume(ThreadLocalRandom.current().nextLong(1000000000L))
				.setVolumeDelta(ThreadLocalRandom.current().nextLong(5000))
				.setTurnover(ThreadLocalRandom.current().nextDouble(1000000000))
				.setTurnoverDelta(ThreadLocalRandom.current().nextDouble(5000))
				.build();
	}
}
