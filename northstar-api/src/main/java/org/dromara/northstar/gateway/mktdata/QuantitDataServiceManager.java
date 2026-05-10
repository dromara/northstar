package org.dromara.northstar.gateway.mktdata;

import java.time.LocalDate;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.datasource.dto.DataSourceMessage.AggBarDTO;
import org.dromara.northstar.datasource.dto.DataSourceMessage.ContractDTO;
import org.dromara.northstar.datasource.dto.DataSourceMessage.HolidayDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * 盈富量化数据服务接口
 * @auth KevinHuangwl
 */
@Slf4j
public class QuantitDataServiceManager {
	
	private static final ParameterizedTypeReference<List<String>> GENIC_MSG_TYPE = new ParameterizedTypeReference<>() {};
	
	private RestTemplate restTemplate;

	public QuantitDataServiceManager(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}
	
	@Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000))
	public List<ContractDTO> getCnFutureContracts() {
		return extract(restTemplate.exchange("/dataservice/contracts/pb/future/cn", HttpMethod.GET, null, GENIC_MSG_TYPE), ContractDTO.class);
	}
	
	@Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000))
	public List<ContractDTO> getForexContracts() {
		return extract(restTemplate.exchange("/dataservice/contracts/pb/forex", HttpMethod.GET, null, GENIC_MSG_TYPE), ContractDTO.class);
	}
	
	@Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000))
	public List<ContractDTO> getCryptoContracts() {
		return extract(restTemplate.exchange("/dataservice/contracts/pb/crypto", HttpMethod.GET, null, GENIC_MSG_TYPE), ContractDTO.class);
	}
	
	@Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000))
	public List<ContractDTO> getUsStocksContracts() {
		return extract(restTemplate.exchange("/dataservice/contracts/pb/stocks/us", HttpMethod.GET, null, GENIC_MSG_TYPE), ContractDTO.class);
	}
	
	@Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000))
	public List<ContractDTO> getUsETFContracts() {
		return extract(restTemplate.exchange("/dataservice/contracts/pb/etf/us", HttpMethod.GET, null, GENIC_MSG_TYPE), ContractDTO.class);
	}
	
	@Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000))
	public List<ContractDTO> getCnStocksContracts() {
		return extract(restTemplate.exchange("/dataservice/contracts/pb/stocks/cn", HttpMethod.GET, null, GENIC_MSG_TYPE), ContractDTO.class);
	}
	
	@Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000))
	public List<ContractDTO> getHkStocksContracts() {
		return extract(restTemplate.exchange("/dataservice/contracts/pb/stocks/hk", HttpMethod.GET, null, GENIC_MSG_TYPE), ContractDTO.class);
	}
	
	private <T> List<T> extract(ResponseEntity<List<String>> response, Class<T> clazz){
		List<String> body = response.getBody();
		if(body == null) {
			return Collections.emptyList();
		}
		return body.stream()
				.map(data -> {
					try {
						byte[] bytes = Base64.getDecoder().decode(data);
				        return clazz.cast(Class.forName(clazz.getName()).getMethod("parseFrom", byte[].class).invoke(null, bytes));
				    } catch (Exception e) {
				    	log.warn("对象内容：{}", data);
				    	log.warn("实体转换异常：{}", e.getMessage());
				        return null;
				    }
				})
				.toList();
	}
	
	public List<HolidayDTO> getHolidays(int year){
		log.info("请求数据服务查询{}年节假日信息", year);
		ResponseEntity<List<String>> resp = restTemplate.exchange("/dataservice/holidays/pb/{year}",
				HttpMethod.GET, null, GENIC_MSG_TYPE, year);
		return extract(resp, HolidayDTO.class);
	}
	
	@Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000))
	public List<AggBarDTO> getMinutelyData(String unifiedSymbol, LocalDate start, LocalDate end) {
		log.info("请求数据服务1m数据：{} {} -> {}", unifiedSymbol, start, end);
		ResponseEntity<List<String>> resp = restTemplate.exchange("/dataservice/data/pb/min?unifiedSymbol={unifiedSymbol}&startDate={start}&endDate={end}",
				HttpMethod.GET, null, GENIC_MSG_TYPE, unifiedSymbol, start, end);
		return extract(resp, AggBarDTO.class);
	}
	
	@Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000))
	public List<AggBarDTO> getQuarterlyData(String unifiedSymbol, LocalDate start, LocalDate end) {
		log.info("请求数据服务15m数据：{} {} -> {}", unifiedSymbol, start, end);
		ResponseEntity<List<String>> resp = restTemplate.exchange("/dataservice/data/pb/quarter?unifiedSymbol={unifiedSymbol}&startDate={start}&endDate={end}",
				HttpMethod.GET, null, GENIC_MSG_TYPE, unifiedSymbol, start, end);
		return extract(resp, AggBarDTO.class);
	}
	
	@Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000))
	public List<AggBarDTO> getHourlyData(String unifiedSymbol, LocalDate start, LocalDate end) {
		log.info("请求数据服务1h数据：{} {} -> {}", unifiedSymbol, start, end);
		ResponseEntity<List<String>> resp = restTemplate.exchange("/dataservice/data/pb/hour?unifiedSymbol={unifiedSymbol}&startDate={start}&endDate={end}",
				HttpMethod.GET, null, GENIC_MSG_TYPE, unifiedSymbol, start, end);
		return extract(resp, AggBarDTO.class);
	}
	
	@Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000))
	public List<AggBarDTO> getDailyData(String unifiedSymbol, LocalDate start, LocalDate end) {
		log.info("请求数据服务1d数据：{} {} -> {}", unifiedSymbol, start, end);
		ResponseEntity<List<String>> resp = restTemplate.exchange("/dataservice/data/pb/day?unifiedSymbol={unifiedSymbol}&startDate={start}&endDate={end}",
				HttpMethod.GET, null, GENIC_MSG_TYPE, unifiedSymbol, start, end);
		return extract(resp, AggBarDTO.class);
	}
	
	@Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000))
	public List<AggBarDTO> getData(String unifiedSymbol, Interval interval, LocalDate start, LocalDate end) {
		log.info("请求数据服务{}数据：{} {} -> {}", interval.getAlias(), unifiedSymbol, start, end);
		ResponseEntity<List<String>> resp = restTemplate.exchange("/dataservice/data/pb/{interval}?unifiedSymbol={unifiedSymbol}&startDate={start}&endDate={end}",
				HttpMethod.GET, null, GENIC_MSG_TYPE, interval.getAlias(), unifiedSymbol, start, end);
		return extract(resp, AggBarDTO.class);
	}
}
