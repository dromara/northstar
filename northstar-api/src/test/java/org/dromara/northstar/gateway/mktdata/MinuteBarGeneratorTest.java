package org.dromara.northstar.gateway.mktdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedList;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.CommonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MinuteBarGeneratorTest {
	
	MinuteBarGenerator barGen;
	
	LinkedList<Bar> results = new LinkedList<>();
	
	Contract c = Contract.builder()
			.name("测试合约")
			.unifiedSymbol("test2405@TEST@FUTURES")
			.channelType(ChannelType.CTP)
			.build();
	
	@BeforeEach
	void prepare() {
		results.clear();
		barGen = new MinuteBarGenerator(c, bar -> results.add(bar));
	}
	
	@Test
	void testSHFEDuringMarketTime() {
		LocalDate date = LocalDate.now();
		LocalTime time = LocalTime.now().withMinute(0).withSecond(0).withNano(0);
		for(int i=0; i<=120; i++) {
			time = time.plusNanos(500*1000000);
			long timestamp = CommonUtils.localDateTimeToMills(LocalDateTime.of(date, time));
			System.out.println("time:" + time);
			barGen.update(Tick.builder()
					.contract(c)
					.tradingDay(date)
					.actionDay(date)
					.actionTime(time)
					.actionTimestamp(timestamp)
					.channelType(ChannelType.CTP)
					.type(TickType.MARKET_TICK)
					.build());
		}
		assertThat(results).isNotEmpty();
	}
	
	@Test
	void testSHFEClosingMarketTime() throws InterruptedException {
		LocalDate date = LocalDate.now();
		LocalTime time = LocalTime.now().withMinute(0).withSecond(0).withNano(0);
		for(int i=0; i<120; i++) {
			time = time.plusNanos(500*1000000);
			long timestamp = CommonUtils.localDateTimeToMills(LocalDateTime.of(date, time));
			System.out.println("time:" + time);
			barGen.update(Tick.builder()
					.contract(c)
					.tradingDay(date)
					.actionDay(date)
					.actionTime(time)
					.actionTimestamp(timestamp)
					.channelType(ChannelType.CTP)
					.type(TickType.MARKET_TICK)
					.build());
		}
		Thread.sleep(60000);
		assertThat(results).isNotEmpty();
	}

}
