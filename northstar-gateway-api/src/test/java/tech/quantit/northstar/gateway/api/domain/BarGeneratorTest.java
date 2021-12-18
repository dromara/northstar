package tech.quantit.northstar.gateway.api.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.constant.TickType;
import tech.quantit.northstar.common.utils.MarketTimeUtil;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.TickField;

class BarGeneratorTest {

	private TickField.Builder proto;
	
	private MarketTimeUtil util = new CtpMarketTimeUtil();
	
	private NormalContract contract = mock(NormalContract.class);
	
	private TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	@BeforeEach
	public void prepare() {
		proto = TickField.newBuilder()
				.setUnifiedSymbol("rb2101")
				.setGatewayId("testGateway")
				.setTradingDay("20210619")
				.setActionDay("20210618");
		
		when(contract.unifiedSymbol()).thenReturn("rb2101");
		when(contract.contractField()).thenReturn(factory.makeContract("rb2101"));
	}

	/**
	 * 验证开盘时段
	 */
	@Test
	public void testOpeningScene() {
		AtomicInteger cnt = new AtomicInteger();
		List<String> timeList = List.of("2100");
		BarGenerator bg = new BarGenerator(contract, (bar) -> {
			assertThat(bar.getActionTime().substring(0, 4)).isEqualTo(timeList.get(cnt.getAndIncrement()));
		});
		LocalDateTime ldt = LocalDateTime.of(2021, 6, 18, 20, 59, 0, 0);
		LocalDateTime endTime = LocalDateTime.of(2021, 6, 18, 21, 1, 0, 1);
		runner(bg, ldt, endTime);
		assertThat(cnt.get()).isEqualTo(1);
	}
	
	/**
	 * 验证一般时段
	 */
	@Test
	public void testNormalScene() {
		AtomicInteger cnt = new AtomicInteger();
		List<String> timeList = List.of("2100", "2101", "2102");
		BarGenerator bg = new BarGenerator(contract, (bar) -> {
			assertThat(bar.getActionTime().substring(0, 4)).isEqualTo(timeList.get(cnt.getAndIncrement()));
		});
		LocalDateTime ldt = LocalDateTime.of(2021, 6, 18, 20, 59, 0, 0);
		LocalDateTime endTime = LocalDateTime.of(2021, 6, 18, 21, 3, 0, 1);
		runner(bg, ldt, endTime);
		assertThat(cnt.get()).isEqualTo(3);
	}

	/**
	 * 验证跨小节时段0
	 */
	@Test
	public void testCrossSectionScene0() {
		AtomicInteger cnt = new AtomicInteger();
		List<String> timeList = List.of("2255", "2256", "2257", "2258", "2259", "0900", "0901");
		BarGenerator bg = new BarGenerator(contract, (bar) -> {
			assertThat(bar.getActionTime().substring(0, 4)).isEqualTo(timeList.get(cnt.getAndIncrement()));
		});
		LocalDateTime ldt = LocalDateTime.of(2021, 6, 18, 22, 55, 0, 0);
		LocalDateTime endTime = LocalDateTime.of(2021, 6, 18, 23, 0, 0, 1);
		runner(bg, ldt, endTime);
		assertThat(cnt.get()).isEqualTo(5);
		LocalDateTime ldt2 = LocalDateTime.of(2021, 6, 19, 9, 0, 0, 0);
		LocalDateTime endTime2 = LocalDateTime.of(2021, 6, 19, 9, 2, 0, 1);
		runner(bg, ldt2, endTime2);
		assertThat(cnt.get()).isEqualTo(7);
	}
	
	/**
	 * 验证跨小节时段1
	 */
	@Test
	public void testCrossSectionScene1() {
		AtomicInteger cnt = new AtomicInteger();
		List<String> timeList = List.of("2255", "2256", "2257", "2258", "2259", "0900", "0901");
		BarGenerator bg = new BarGenerator(contract, (bar) -> {
			assertThat(bar.getActionTime().substring(0, 4)).isEqualTo(timeList.get(cnt.getAndIncrement()));
		});
		LocalDateTime ldt = LocalDateTime.of(2021, 6, 18, 22, 55, 0, 0);
		LocalDateTime endTime = LocalDateTime.of(2021, 6, 18, 23, 0, 0, 0);
		runner(bg, ldt, endTime);
		assertThat(cnt.get()).isEqualTo(4);
		LocalDateTime ldt2 = LocalDateTime.of(2021, 6, 19, 9, 0, 0, 0);
		LocalDateTime endTime2 = LocalDateTime.of(2021, 6, 19, 9, 2, 0, 1);
		runner(bg, ldt2, endTime2);
		assertThat(cnt.get()).isEqualTo(7);
	}
	
	/**
	 * 验证跨小节时段2
	 */
	@Test
	public void testCrossSectionScene2() {
		AtomicInteger cnt = new AtomicInteger();
		List<String> timeList = List.of("1128", "1129", "1330", "1331", "1332");
		BarGenerator bg = new BarGenerator(contract, (bar) -> {
			assertThat(bar.getActionTime().substring(0, 4)).isEqualTo(timeList.get(cnt.getAndIncrement()));
		});
		LocalDateTime ldt = LocalDateTime.of(2021, 6, 19, 11, 28, 0, 0);
		LocalDateTime endTime = LocalDateTime.of(2021, 6, 19, 11, 30, 0, 0);
		runner(bg, ldt, endTime);
		assertThat(cnt.get()).isEqualTo(1);
		LocalDateTime ldt2 = LocalDateTime.of(2021, 6, 19, 13, 30, 0, 0);
		LocalDateTime endTime2 = LocalDateTime.of(2021, 6, 19, 13, 33, 0, 1);
		runner(bg, ldt2, endTime2);
		assertThat(cnt.get()).isEqualTo(5);
	}
	
	/**
	 * 验证跨小节时段3
	 */
	@Test
	public void testCrossSectionScene3() {
		AtomicInteger cnt = new AtomicInteger();
		List<String> timeList = List.of("1128", "1129", "1330", "1331", "1332");
		BarGenerator bg = new BarGenerator(contract, (bar) -> {
			assertThat(bar.getActionTime().substring(0, 4)).isEqualTo(timeList.get(cnt.getAndIncrement()));
		});
		LocalDateTime ldt = LocalDateTime.of(2021, 6, 19, 11, 28, 0, 0);
		LocalDateTime endTime = LocalDateTime.of(2021, 6, 19, 11, 30, 0, 1);
		runner(bg, ldt, endTime);
		assertThat(cnt.get()).isEqualTo(2);
		LocalDateTime ldt2 = LocalDateTime.of(2021, 6, 19, 13, 30, 0, 0);
		LocalDateTime endTime2 = LocalDateTime.of(2021, 6, 19, 13, 33, 0, 1);
		runner(bg, ldt2, endTime2);
		assertThat(cnt.get()).isEqualTo(5);
	}
	
	/**
	 * 验证收盘时段
	 */
	@Test
	public void testClosingScene() {
		AtomicInteger cnt = new AtomicInteger();
		List<String> timeList = List.of("1455", "1456", "1457", "1458", "1459");
		BarGenerator bg = new BarGenerator(contract, (bar) -> {
			assertThat(bar.getActionTime().substring(0, 4)).isEqualTo(timeList.get(cnt.getAndIncrement()));
		});
		LocalDateTime ldt = LocalDateTime.of(2021, 6, 18, 14, 55, 0, 0);
		LocalDateTime endTime = LocalDateTime.of(2021, 6, 18, 15, 0, 0, 1);
		runner(bg, ldt, endTime);
		assertThat(cnt.get()).isEqualTo(5);
	}
	
	/**
	 * 验证收盘时段2
	 */
	@Test
	public void testClosingScene2() {
		AtomicInteger cnt = new AtomicInteger();
		List<String> timeList = List.of("1455", "1456", "1457", "1458", "1459", "1500",
				"1501", "1502", "1503", "1504", "1505", "1506", "1507", "1508",
				"1509", "1510", "1511", "1512", "1513", "1514");
		BarGenerator bg = new BarGenerator(contract, (bar) -> {
			assertThat(bar.getActionTime().substring(0, 4)).isEqualTo(timeList.get(cnt.getAndIncrement()));
		});
		LocalDateTime ldt = LocalDateTime.of(2021, 6, 18, 14, 55, 0, 0);
		LocalDateTime endTime = LocalDateTime.of(2021, 6, 18, 15, 15, 0, 1);
		runner(bg, ldt, endTime);
		assertThat(cnt.get()).isEqualTo(20);
	}
	

	private void runner(BarGenerator bg, LocalDateTime startTime, LocalDateTime endTime) {
		while(startTime.isBefore(endTime)) {
			proto.setActionDay(startTime.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
			proto.setActionTime(startTime.format(DateTimeConstant.T_FORMAT_INT_FORMATTER));
			proto.setActionTimestamp(startTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
			proto.setStatus(util.resolveTickType(LocalTime.from(startTime)).getCode());
			
			bg.update(proto.build());
			startTime = startTime.plusNanos(500000000);
		}
	}

	class CtpMarketTimeUtil implements MarketTimeUtil{
		
		LocalTime nightMarketStartTime = LocalTime.of(20, 58, 59, 999999999);
		LocalTime nightMarketOpenTime = LocalTime.of(21, 0, 1);
		LocalTime nightMarketEndTime = LocalTime.of(2, 30, 0, 999999);
		LocalTime dayMarketStartTime = LocalTime.of(8, 58, 59, 999999999);
		LocalTime dayMarketOpenTime = LocalTime.of(9, 0, 1);
		LocalTime dayMarketClosingTime1 = LocalTime.of(15, 0, 0);
		LocalTime dayMarketClosingTime2 = LocalTime.of(15, 15, 0);
		LocalTime dayMarketEndTime = LocalTime.of(15, 15, 0, 999999);
		
		long LESS_THEN_HALF_SEC_IN_NANO = 400000000;
		/**
		 * 根据时间判定Tick类型
		 */
		@Override
		public TickType resolveTickType(LocalTime time) {
			
			if(time.isAfter(nightMarketEndTime) && time.isBefore(dayMarketStartTime)
					|| time.isAfter(dayMarketEndTime) && time.isBefore(nightMarketStartTime)) {
				return TickType.NON_OPENING_TICK;
			}
			if(time.isAfter(nightMarketStartTime) && time.isBefore(nightMarketOpenTime)
					|| time.isAfter(dayMarketStartTime) && time.isBefore(dayMarketOpenTime)) {
				return TickType.PRE_OPENING_TICK;
			}
			if(Math.abs(dayMarketClosingTime1.toNanoOfDay() - time.toNanoOfDay()) <= LESS_THEN_HALF_SEC_IN_NANO
					|| Math.abs(dayMarketClosingTime2.toNanoOfDay() - time.toNanoOfDay()) <= LESS_THEN_HALF_SEC_IN_NANO) {
				return TickType.CLOSING_TICK;
			}
			if(time.getSecond() == 0 && time.getNano() == 0) {
				return TickType.END_OF_MIN_TICK;
			}
			return TickType.NORMAL_TICK;
		}

	}
}
