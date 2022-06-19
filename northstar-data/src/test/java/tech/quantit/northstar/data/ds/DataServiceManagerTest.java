package tech.quantit.northstar.data.ds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;

import tech.quantit.northstar.common.utils.MarketDateTimeUtil;
import tech.quantit.northstar.data.ds.DataServiceManager.DataSet;
import xyz.redtorch.pb.CoreField.BarField;

@SuppressWarnings("unchecked")
class DataServiceManagerTest {
	
	DataServiceManager mgr;
	
	@Test
	void testDailyData() {
		RestTemplate rest = mock(RestTemplate.class);
		ResponseEntity<String> mockResp = mock(ResponseEntity.class);
		when(mockResp.getBody()).thenReturn("");
		when(rest.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), eq(String.class))).thenReturn(mockResp);
		MarketDateTimeUtil util = mock(MarketDateTimeUtil.class);
		mgr = new DataServiceManager("", "", rest, util);
		String data = "{\"fields\":[\"ns_code\",\"trade_date\",\"pre_close\",\"pre_settle\",\"open\",\"high\",\"low\",\"close\",\"settle\",\"change1\",\"change2\",\"vol\",\"amount\",\"oi\",\"oi_chg\"],\"items\":[[\"rb2205@SHFE\",\"20220215\",\"4817.0\",\"4862.0\",\"4805.0\",\"4816.0\",\"4666.0\",\"4728.0\",\"4744.0\",\"-134.0\",\"-118.0\",\"2124511.0\",\"10079013.32\",\"1921187.0\",\"32370.0\"],[\"rb2205@SHFE\",\"20220214\",\"4905.0\",\"4986.0\",\"4900.0\",\"4910.0\",\"4791.0\",\"4817.0\",\"4862.0\",\"-169.0\",\"-124.0\",\"1874764.0\",\"9116170.34\",\"1888817.0\",\"-6322.0\"]]}";
		DataSet dataSet = JSON.parseObject(data, DataSet.class);
		when(util.getTradingDay(any(LocalDateTime.class))).thenReturn(LocalDate.now());
		ResponseEntity<DataSet> mockResp2 = mock(ResponseEntity.class);
		when(mockResp2.getBody()).thenReturn(dataSet);
		when(mockResp2.getStatusCode()).thenReturn(HttpStatus.OK);
		when(rest.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
			.thenReturn(mockResp2);
		
		List<BarField> result = mgr.getDailyData("test", LocalDate.now(), LocalDate.now());
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getActionDay()).isEqualTo("20220214");
		assertThat(result.get(0).getActionTime()).isEqualTo("09:00:00");
		assertThat(result.get(0).getTradingDay()).isEqualTo("20220214");
		assertThat(result.get(0).getActionTimestamp() > 0).isTrue();
		assertThat(result.get(0).getUnifiedSymbol()).isEqualTo("rb2205@SHFE");
	}
	
	@Test
	void testHourlyData() {
		RestTemplate rest = mock(RestTemplate.class);
		ResponseEntity<String> mockResp = mock(ResponseEntity.class);
		when(mockResp.getBody()).thenReturn("");
		when(rest.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), eq(String.class))).thenReturn(mockResp);
		MarketDateTimeUtil util = mock(MarketDateTimeUtil.class);
		mgr = new DataServiceManager("", "", rest, util);
		String data = "{\"fields\":[\"ns_code\",\"trade_time\",\"open\",\"close\",\"high\",\"low\",\"vol\",\"amount\",\"oi\"],\"items\":[[\"rb2205@SHFE\",\"2022-02-15 15:00:00\",\"4690.0\",\"4728.0\",\"4741.0\",\"4688.0\",\"243861.0\",\"11508913810.0\",\"1921187.0\"],[\"rb2205@SHFE\",\"2022-02-15 14:15:00\",\"4755.0\",\"4692.0\",\"4760.0\",\"4666.0\",\"750003.0\",\"35311449470.0\",\"1912611.0\"],[\"rb2205@SHFE\",\"2022-02-15 11:15:00\",\"4749.0\",\"4755.0\",\"4795.0\",\"4743.0\",\"246912.0\",\"11779822690.0\",\"1855328.0\"],[\"rb2205@SHFE\",\"2022-02-15 10:00:00\",\"4770.0\",\"4750.0\",\"4787.0\",\"4733.0\",\"317443.0\",\"15098072220.0\",\"1838341.0\"],[\"rb2205@SHFE\",\"2022-02-14 23:00:00\",\"4790.0\",\"4772.0\",\"4795.0\",\"4765.0\",\"142364.0\",\"6808136940.0\",\"1823296.0\"],[\"rb2205@SHFE\",\"2022-02-14 22:00:00\",\"4801.0\",\"4790.0\",\"4816.0\",\"4763.0\",\"419865.0\",\"20088510910.0\",\"1828685.0\"],[\"rb2205@SHFE\",\"2022-02-14 21:00:00\",\"4805.0\",\"4805.0\",\"4805.0\",\"4805.0\",\"4063.0\",\"195227150.0\",\"1887841.0\"]]}";
		DataSet dataSet = JSON.parseObject(data, DataSet.class);
		ResponseEntity<DataSet> mockResp2 = mock(ResponseEntity.class);
		when(util.getTradingDay(any(LocalDateTime.class))).thenReturn(LocalDate.of(2022, 2, 15));
		when(mockResp2.getBody()).thenReturn(dataSet);
		when(mockResp2.getStatusCode()).thenReturn(HttpStatus.OK);
		when(rest.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
			.thenReturn(mockResp2);
		
		List<BarField> result = mgr.getHourlyData("test", LocalDate.now(), LocalDate.now());
		assertThat(result).hasSize(6);
		assertThat(result.get(0).getActionDay()).isEqualTo("20220214");
		assertThat(result.get(0).getActionTime()).isEqualTo("22:00:00");
		assertThat(result.get(0).getTradingDay()).isEqualTo("20220215");
		assertThat(result.get(0).getActionTimestamp() > 0).isTrue();
		assertThat(result.get(0).getUnifiedSymbol()).isEqualTo("rb2205@SHFE");
	}

}
