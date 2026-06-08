package org.dromara.northstar.app.restful;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.model.BarVO;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.gateway.playback.PlaybackGatewaySettings;
import org.dromara.northstar.gateway.playback.model.PlaybackPrecision;
import org.dromara.northstar.gateway.playback.model.PlaybackSpeed;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=unittest")
@AutoConfigureMockMvc
class MarketDataControllerTest {

	@Autowired
	MockMvc mvc;
	
	@Autowired
	GatewayController gatewayCtrlr;
	
	GatewayDescription gd = GatewayDescription.builder()
			.gatewayId("PLAYBACK")
			.channelType(ChannelType.PLAYBACK)
			.gatewayUsage(GatewayUsage.MARKET_DATA)
			.subscribedContracts(List.of(ContractSimpleInfo.builder()
					.id("BTCUSDT@BINANCE@SWAP@PLAYBACK")
					.unifiedSymbol("BTCUSDT@BINANCE@SWAP")
					.channelType(ChannelType.PLAYBACK)
					.build()))
			.settings(PlaybackGatewaySettings.builder()
					.startDate(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
					.endDate(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
					.speed(PlaybackSpeed.NORMAL)
					.precision(PlaybackPrecision.LOW)
					.build())
			.build();
	
	@BeforeEach
	void prepare() {
		gatewayCtrlr.createGateway(gd);
	}
	
	@AfterEach
	void cleanup() {
		gatewayCtrlr.removeGateway("PLAYBACK");
	}
	
	@Test
	void testRange() throws Exception {
		String result = mvc.perform(MockMvcRequestBuilders.get("/northstar/marketdata/range/BTCUSDT@BINANCE@SWAP@PLAYBACK/15m?fromTime=&toTime="))
		.andExpect(status().isOk())
		.andReturn().getResponse().getContentAsString();
		ObjectMapper mapper = new ObjectMapper();
		List<BarVO> bars = mapper.readValue(result, new TypeReference<List<BarVO>>(){});
		
		assertThat(bars).isNotEmpty();
	}
	
	@Test
	void testRangeCrypto() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime lastWeek = now.minusWeeks(2);
		String timeRange = String.format("?fromTime=%d&toTime=%d", CommonUtils.localDateTimeToMills(lastWeek), CommonUtils.localDateTimeToMills(now));
		String result = mvc.perform(MockMvcRequestBuilders.get("/northstar/marketdata/range/BTCUSDT@BINANCE@SWAP@PLAYBACK/1wk" + timeRange))
		.andExpect(status().isOk())
		.andReturn().getResponse().getContentAsString();
		ObjectMapper mapper = new ObjectMapper();
		List<BarVO> bars = mapper.readValue(result, new TypeReference<List<BarVO>>(){});
		
		assertThat(bars).isNotEmpty();
		Set<String> dates = new HashSet<>();
		bars.forEach(bar -> {
			assertThat(dates.contains(bar.getTradingDay()));
			dates.add(bar.getTradingDay());
			assertThat(LocalDate.parse(bar.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
		});
	}

	@Test
	void testRangeDailyCrypto() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime lastWeek = now.minusWeeks(1);
		String timeRange = String.format("?fromTime=%d&toTime=%d", CommonUtils.localDateTimeToMills(lastWeek), CommonUtils.localDateTimeToMills(now));
		String result = mvc.perform(MockMvcRequestBuilders.get("/northstar/marketdata/range/BTCUSDT@BINANCE@SWAP@PLAYBACK/1d" + timeRange))
		.andExpect(status().isOk())
		.andReturn().getResponse().getContentAsString();
		ObjectMapper mapper = new ObjectMapper();
		List<BarVO> bars = mapper.readValue(result, new TypeReference<List<BarVO>>(){});
		
		assertThat(bars).isNotEmpty();
		Set<String> dates = new HashSet<>();
		bars.forEach(bar -> {
			assertThat(dates.contains(bar.getTradingDay()));
			dates.add(bar.getTradingDay());
		});
	}
	
	@Test
	void testRangeMinutelyCrypto() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime lastWeek = now.minusWeeks(1);
		String timeRange = String.format("?fromTime=%d&toTime=%d", CommonUtils.localDateTimeToMills(lastWeek), CommonUtils.localDateTimeToMills(now));
		String result = mvc.perform(MockMvcRequestBuilders.get("/northstar/marketdata/range/BTCUSDT@BINANCE@SWAP@PLAYBACK/1m" + timeRange))
		.andExpect(status().isOk())
		.andReturn().getResponse().getContentAsString();
		ObjectMapper mapper = new ObjectMapper();
		List<BarVO> bars = mapper.readValue(result, new TypeReference<List<BarVO>>(){});
		
		assertThat(bars).isNotEmpty();
		Set<Long> times = new HashSet<>();
		bars.forEach(bar -> {
			assertThat(times.contains(bar.getTimestamp()));
			times.add(bar.getTimestamp());
		});
	}
	
	@Test
	void testRangeQuarterlyCrypto() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime lastWeek = now.minusWeeks(1);
		String timeRange = String.format("?fromTime=%d&toTime=%d", CommonUtils.localDateTimeToMills(lastWeek), CommonUtils.localDateTimeToMills(now));
		String result = mvc.perform(MockMvcRequestBuilders.get("/northstar/marketdata/range/BTCUSDT@BINANCE@SWAP@PLAYBACK/15m" + timeRange))
		.andExpect(status().isOk())
		.andReturn().getResponse().getContentAsString();
		ObjectMapper mapper = new ObjectMapper();
		List<BarVO> bars = mapper.readValue(result, new TypeReference<List<BarVO>>(){});
		
		assertThat(bars).isNotEmpty();
		Set<Long> times = new HashSet<>();
		bars.forEach(bar -> {
			assertThat(times.contains(bar.getTimestamp()));
			times.add(bar.getTimestamp());
		});
	}
}
