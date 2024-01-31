package org.dromara.northstar.gateway.mktdata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.gateway.mktdata.NorthstarDataServiceDataSource.DataSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;

class NorthstarDataServiceDataSourceTest {

    private RestTemplate restTemplate = mock(RestTemplate.class);

    private NorthstarDataServiceDataSource dataSource;
    
    private LocalDate DATE = LocalDate.now();
    
    private DataSet dummyDS = new DataSet();

    @BeforeEach
    public void setUp() {
        dataSource = new NorthstarDataServiceDataSource("http://base.url", "secretToken", restTemplate);
        dummyDS.setFields(new String[0]);
        dummyDS.setItems(new String[0][0]);
    }
    
    @Test
    void testRegister() {
    	ResponseEntity<String> resp = new ResponseEntity<>("something", HttpStatus.OK);
    	when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), eq(String.class))).thenReturn(resp);
    	assertDoesNotThrow(() -> {
    		dataSource.register();
    	});
    }
    
	@Test
	void testGetMinutelyData() {
		ResponseEntity<DataSet> resp = new ResponseEntity<>(dummyDS, HttpStatus.OK);
    	when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), eq(DataSet.class))).thenReturn(resp);
    	assertDoesNotThrow(() -> {
    		dataSource.getMinutelyData(Contract.builder().unifiedSymbol("test").build(), DATE, DATE);
    	});
	}

	@Test
	void testGetQuarterlyData() {
		ResponseEntity<DataSet> resp = new ResponseEntity<>(dummyDS, HttpStatus.OK);
    	when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), eq(DataSet.class))).thenReturn(resp);
    	assertDoesNotThrow(() -> {
    		dataSource.getQuarterlyData(Contract.builder().unifiedSymbol("test").build(), DATE, DATE);
    	});
	}

	@Test
	void testGetHourlyData() {
		ResponseEntity<DataSet> resp = new ResponseEntity<>(dummyDS, HttpStatus.OK);
    	when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), eq(DataSet.class))).thenReturn(resp);
    	assertDoesNotThrow(() -> {
    		dataSource.getHourlyData(Contract.builder().unifiedSymbol("test").build(), DATE, DATE);
    	});
	}

	@Test
	void testGetDailyData() {
		ResponseEntity<DataSet> resp = new ResponseEntity<>(dummyDS, HttpStatus.OK);
    	when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), eq(DataSet.class))).thenReturn(resp);
    	assertDoesNotThrow(() -> {
    		dataSource.getDailyData(Contract.builder().unifiedSymbol("test").build(), DATE, DATE);
    	});
	}

	@Test
	void testGetHolidays() {
		ResponseEntity<DataSet> resp = new ResponseEntity<>(dummyDS, HttpStatus.OK);
    	when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), eq(DataSet.class))).thenReturn(resp);
    	assertDoesNotThrow(() -> {
    		dataSource.getHolidays(ExchangeEnum.BINANCE, DATE, DATE);
    	});
	}

	@Test
	void testGetAllContracts() {
		ResponseEntity<DataSet> resp = new ResponseEntity<>(dummyDS, HttpStatus.OK);
    	when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), eq(DataSet.class))).thenReturn(resp);
    	assertDoesNotThrow(() -> {
    		dataSource.getAllContracts(ExchangeEnum.BINANCE);
    	});
	}

	@SuppressWarnings("rawtypes")
	@Test
	void testGetUserAvailableExchanges() {
		ResponseEntity<List> resp = new ResponseEntity<>(List.of(), HttpStatus.OK);
    	when(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), eq(List.class))).thenReturn(resp);
    	assertDoesNotThrow(() -> {
    		dataSource.getUserAvailableExchanges();
    	});
	}

}
