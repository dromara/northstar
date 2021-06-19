package tech.xuanwu.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;

import tech.xuanwu.northstar.common.constant.DateTimeConstant;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpMarketTimeUtil;
import xyz.redtorch.pb.CoreField.TickField;

public class BarGeneratorTest {

	private TickField.Builder proto;
	
	private MarketTimeUtil util = new CtpMarketTimeUtil();
	
	@Before
	public void prepare() {
		System.out.println("准备");
		proto = TickField.newBuilder()
				.setUnifiedSymbol("rb2101")
				.setGatewayId("testGateway")
				.setTradingDay("20210619")
				.setActionDay("20210618");
	}

	/**
	 * 验证开盘时段
	 */
	@Test
	public void testOpeningScene() {
		AtomicInteger cnt = new AtomicInteger();
		List<String> timeList = List.of("2100");
		BarGenerator bg = new BarGenerator("rb2101", (bar, tickList) -> {
			assertThat(bar.getActionTime().substring(0, 4)).isEqualTo(timeList.get(cnt.getAndIncrement()));
			assertThat(tickList.size()).isEqualTo(240);
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
		BarGenerator bg = new BarGenerator("rb2101", (bar, tickList) -> {
			assertThat(bar.getActionTime().substring(0, 4)).isEqualTo(timeList.get(cnt.getAndIncrement()));
			if(cnt.get() == 1) {
				assertThat(tickList.size()).isEqualTo(240);
			}else {
				assertThat(tickList.size()).isCloseTo(120, Offset.offset(1));
			}
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
		BarGenerator bg = new BarGenerator("rb2101", (bar, tickList) -> {
			assertThat(bar.getActionTime().substring(0, 4)).isEqualTo(timeList.get(cnt.getAndIncrement()));
			assertThat(tickList.size()).isCloseTo(120, Offset.offset(1));
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
		BarGenerator bg = new BarGenerator("rb2101", (bar, tickList) -> {
			assertThat(bar.getActionTime().substring(0, 4)).isEqualTo(timeList.get(cnt.getAndIncrement()));
			assertThat(tickList.size()).isCloseTo(120, Offset.offset(1));
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
		BarGenerator bg = new BarGenerator("rb2101", (bar, tickList) -> {
			assertThat(bar.getActionTime().substring(0, 4)).isEqualTo(timeList.get(cnt.getAndIncrement()));
			assertThat(tickList.size()).isCloseTo(120, Offset.offset(1));
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
		BarGenerator bg = new BarGenerator("rb2101", (bar, tickList) -> {
			assertThat(bar.getActionTime().substring(0, 4)).isEqualTo(timeList.get(cnt.getAndIncrement()));
			assertThat(tickList.size()).isCloseTo(120, Offset.offset(1));
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
		BarGenerator bg = new BarGenerator("rb2101", (bar, tickList) -> {
			assertThat(bar.getActionTime().substring(0, 4)).isEqualTo(timeList.get(cnt.getAndIncrement()));
			assertThat(tickList.size()).isCloseTo(120, Offset.offset(1));
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
		BarGenerator bg = new BarGenerator("rb2101", (bar, tickList) -> {
			assertThat(bar.getActionTime().substring(0, 4)).isEqualTo(timeList.get(cnt.getAndIncrement()));
			assertThat(tickList.size()).isCloseTo(120, Offset.offset(1));
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
			
			bg.updateTick(proto.build());
			startTime = startTime.plusNanos(500000000);
		}
	}
}
