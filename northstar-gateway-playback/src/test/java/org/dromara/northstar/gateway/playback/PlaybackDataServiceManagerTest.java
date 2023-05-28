package org.dromara.northstar.gateway.playback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.dromara.northstar.common.utils.MarketDateTimeUtil;
import org.dromara.northstar.gateway.playback.PlaybackDataServiceManager.DataSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;

class PlaybackDataServiceManagerTest {

	private RestTemplate restTemplate = mock(RestTemplate.class);

	private MarketDateTimeUtil dtUtil = mock(MarketDateTimeUtil.class);

	private PlaybackDataServiceManager service;

	private final String baseUrl = "http://localhost:8080/data";

	private final String dummyToken = "dummyToken";

	private final String userToken = "userToken";

	private TestFieldFactory factory = new TestFieldFactory("testGateway");

	@BeforeEach
	void setUp() {
		ResponseEntity<String> response = mock(ResponseEntity.class);
		when(response.getBody()).thenReturn(userToken);
		when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class))).thenReturn(response);
		when(dtUtil.getTradingDay(any(LocalDateTime.class))).thenReturn(LocalDate.now());
		
		service = new PlaybackDataServiceManager(baseUrl, dummyToken, restTemplate, dtUtil);
	}

	@Test
	void testGetMinutelyData() {
		LocalDate date = LocalDate.of(2023, 5, 27);
		DataSet dataSet = new DataSet();
		dataSet.setFields(
				new String[] { "ns_code", "trade_time", "open", "close", "high", "low", "vol", "amount", "oi" });
		dataSet.setItems(new String[][] { { "rb0000@SHFE@FUTURES", "2023-03-24 15:00:00", "4075.22", "4077.52",
				"4079.05", "4073.96", "11878", "486340490.00", "3022882.00" } });
		ResponseEntity<DataSet> response = mock(ResponseEntity.class);
		when(response.getBody()).thenReturn(dataSet);
		when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(DataSet.class))).thenReturn(response);

		// Act
		List<BarField> playbackDataList = service.getMinutelyData(factory.makeContract("rb0000"), date, date);

		// Assert
		assertEquals(1, playbackDataList.size());
	}

}
