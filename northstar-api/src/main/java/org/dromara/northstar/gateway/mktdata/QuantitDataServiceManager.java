package org.dromara.northstar.gateway.mktdata;

import java.time.LocalDate;

import org.dromara.northstar.common.model.ResultSet;
import org.springframework.web.client.RestTemplate;

/**
 * 盈富量化数据服务接口
 * @auth KevinHuangwl
 */

public class QuantitDataServiceManager {
	
	private RestTemplate restTemplate;

	public QuantitDataServiceManager(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}
	
	public ResultSet getAllFutureContracts() {
		return restTemplate.getForObject("/dataservice/contracts/future", ResultSet.class);
	}
	
	public ResultSet getAllOptionContracts() {
		return restTemplate.getForObject("/dataservice/contracts/option", ResultSet.class);
	}
	
	public ResultSet getCalendarCN(int year) {
		return restTemplate.getForObject("/dataservice/calendar/cn?year={year}", ResultSet.class, year);
	}
	
	public ResultSet getMinutelyData(String unifiedSymbol, LocalDate start, LocalDate end) {
		return restTemplate.getForObject("/dataservice/data/min?unifiedSymbol={unifiedSymbol}&startDate={start}&endDate={end}", 
				ResultSet.class, unifiedSymbol, start, end);
	}
	
	public ResultSet getQuarterlyData(String unifiedSymbol, LocalDate start, LocalDate end) {
		return restTemplate.getForObject("/dataservice/data/quarter?unifiedSymbol={unifiedSymbol}&startDate={start}&endDate={end}", 
				ResultSet.class, unifiedSymbol, start, end);
	}
	
	public ResultSet getHourlyData(String unifiedSymbol, LocalDate start, LocalDate end) {
		return restTemplate.getForObject("/dataservice/data/hour?unifiedSymbol={unifiedSymbol}&startDate={start}&endDate={end}", 
				ResultSet.class, unifiedSymbol, start, end);
	}
	
	public ResultSet getDailyData(String unifiedSymbol, LocalDate start, LocalDate end) {
		return restTemplate.getForObject("/dataservice/data/day?unifiedSymbol={unifiedSymbol}&startDate={start}&endDate={end}", 
				ResultSet.class, unifiedSymbol, start, end);
	}
}
