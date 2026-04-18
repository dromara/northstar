package org.dromara.northstar.app.restful;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.constant.PlaybackPrecision;
import org.dromara.northstar.common.constant.PlaybackSpeed;
import org.dromara.northstar.common.model.BarVO;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.gateway.playback.PlaybackGatewaySettings;
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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(classes = NorthstarApplication.class, value = "spring.profiles.active=unittest,pct")
@AutoConfigureMockMvc
class PlaybackControllerTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	GatewayController gatewayCtrlr;

	GatewayDescription gd = GatewayDescription.builder().gatewayId("PLAYBACK").channelType(ChannelType.PLAYBACK)
			.gatewayUsage(GatewayUsage.MARKET_DATA)
			.subscribedContracts(List.of(ContractSimpleInfo.builder().id("BTCUSDT@BINANCE@SPOT@PLAYBACK")
					.unifiedSymbol("BTCUSDT@BINANCE@SPOT").channelType(ChannelType.PLAYBACK).build()))
			.settings(PlaybackGatewaySettings.builder()
					.startDate(LocalDate.now().minusWeeks(1).format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
					.endDate(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
					.speed(PlaybackSpeed.NORMAL).precision(PlaybackPrecision.LOW).build())
			.build();

	@Autowired
	GatewayManager gatewayMgr;

	@BeforeEach
	void prepare() {
		gatewayCtrlr.createGateway(gd);
	}
	
	@AfterEach
	void cleanup() {
		gatewayCtrlr.removeGateway("PLAYBACK");
	}

	@Test
	void testPreLoadWeeklyCrypto() throws Exception {
		String result = mvc
				.perform(MockMvcRequestBuilders.get("/northstar/playback/history/BTCUSDT@BINANCE@SPOT@PLAYBACK/1wk"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		ObjectMapper mapper = new ObjectMapper();
		List<BarVO> bars = mapper.readValue(result, new TypeReference<List<BarVO>>() {
		});

		assertThat(bars).isNotEmpty();
		Set<String> dates = new HashSet<>();
		bars.forEach(bar -> {
			assertThat(dates.contains(bar.getTradingDay())).isFalse();
			dates.add(bar.getTradingDay());
			assertThat(LocalDate.parse(bar.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER).getDayOfWeek())
					.isEqualTo(DayOfWeek.MONDAY);
		});
	}

	@Test
	void testPreLoadDailyCrypto() throws Exception {
		String result = mvc
				.perform(MockMvcRequestBuilders.get("/northstar/playback/history/BTCUSDT@BINANCE@SPOT@PLAYBACK/1d"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		ObjectMapper mapper = new ObjectMapper();
		List<BarVO> bars = mapper.readValue(result, new TypeReference<List<BarVO>>() {
		});

		assertThat(bars).isNotEmpty();
		Set<String> dates = new HashSet<>();
		bars.forEach(bar -> {
			assertThat(dates.contains(bar.getTradingDay())).isFalse();
			dates.add(bar.getTradingDay());
		});
	}

	@Test
	void testPreLoadQuarterlyCrypto() throws Exception {
		String result = mvc
				.perform(MockMvcRequestBuilders.get("/northstar/playback/history/BTCUSDT@BINANCE@SPOT@PLAYBACK/15m"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		ObjectMapper mapper = new ObjectMapper();
		List<BarVO> bars = mapper.readValue(result, new TypeReference<List<BarVO>>() {
		});

		assertThat(bars).isNotEmpty();
		Set<Long> times = new HashSet<>();
		bars.forEach(bar -> {
			assertThat(times.contains(bar.getTimestamp())).isFalse();
			times.add(bar.getTimestamp());
		});
	}

	@Test
	void testPreLoadHourlyForex() throws Exception {
		String result = mvc
				.perform(MockMvcRequestBuilders.get("/northstar/playback/history/XAUUSD@FXCM@FOREX@PLAYBACK/4h"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		ObjectMapper mapper = new ObjectMapper();
		List<BarVO> bars = mapper.readValue(result, new TypeReference<List<BarVO>>() {
		});

		assertThat(bars).isNotEmpty();
		Set<Long> times = new HashSet<>();
		bars.forEach(bar -> {
			assertThat(times.contains(bar.getTimestamp()));
			times.add(bar.getTimestamp());
			log.info("{}", CommonUtils.millsToLocalDateTime(bar.getTimestamp()));
		});
	}
	
}
