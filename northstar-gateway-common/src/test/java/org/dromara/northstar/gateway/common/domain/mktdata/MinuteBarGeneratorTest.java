package org.dromara.northstar.gateway.common.domain.mktdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.function.Consumer;

import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.gateway.common.domain.mktdata.MinuteBarGenerator;
import org.dromara.northstar.gateway.common.domain.time.GenericTradeTime;
import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

class MinuteBarGeneratorTest {

	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	@SuppressWarnings("unchecked")
	@Test
	void test() {
		ContractField contract = factory.makeContract("rb2210");
		MinuteBarGenerator gen = new MinuteBarGenerator(contract, new GenericTradeTime(), mock(Consumer.class));
		LocalDateTime tickDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 55));
		LocalDateTime end = tickDateTime.plusDays(1);
		System.out.println("开始时间：" + tickDateTime.toLocalTime());
		System.out.println("结束时间：" + end.toLocalTime());
		int i = 0;
		while(tickDateTime.isBefore(end)) {
			System.out.println(tickDateTime);
			long time = tickDateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
			long barTime = time - time % 60000 + 60000;
			TickField tick1 = TickField.newBuilder()
					.setUnifiedSymbol(contract.getUnifiedSymbol())
					.setActionDay(tickDateTime.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
					.setActionTime(tickDateTime.format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER))
					.setActionTimestamp(time)
					.setLastPrice(10000)
					.setStatus(1)
					.build();
			
			gen.update(tick1);
			i++;
			BarField bar = gen.finishOfBar();
			assertThat(bar.getActionTimestamp()).isEqualTo(barTime);
			assertThat(bar.getActionTime()).isEqualTo(LocalDateTime.ofInstant(Instant.ofEpochMilli(barTime), ZoneId.systemDefault()).format(DateTimeConstant.T_FORMAT_FORMATTER));
			tickDateTime = tickDateTime.plusMinutes(1);
		}
		assertThat(i).isEqualTo(60*24);
	}

}
