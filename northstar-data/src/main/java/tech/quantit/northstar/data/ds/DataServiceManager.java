package tech.quantit.northstar.data.ds;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.IDataServiceManager;
import tech.quantit.northstar.common.utils.LocalEnvUtils;
import tech.quantit.northstar.common.utils.MarketDateTimeUtil;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 历史数据服务接口管理器
 * @author KevinHuangwl
 *
 */

@Slf4j
public class DataServiceManager implements IDataServiceManager {
	
	private String nsSecret;

	private String baseUrl;
	
	private volatile String userToken;
	
	private DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
	
	private DateTimeFormatter dtfmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	private DateTimeFormatter dtfmt2 = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
	
	private DateTimeFormatter tfmt = DateTimeFormatter.ofPattern("HH:mm:ss");
	
	private MarketDateTimeUtil dtUtil;
	
	private RestTemplate restTemplate;
	
	public DataServiceManager(String baseUrl, String secret, RestTemplate restTemplate, MarketDateTimeUtil dtUtil) {
		this.baseUrl =  baseUrl;
		this.nsSecret = secret;
		this.dtUtil = dtUtil;
		this.restTemplate = restTemplate;
		log.info("采用外部数据源加载历史数据");
		register();
	}
	
	private void register() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-SECRET", nsSecret);
		headers.add("X-MACHINE", LocalEnvUtils.getMACAddress());
		HttpEntity<?> reqEntity = new HttpEntity<>(headers);
		ResponseEntity<String> respEntity = restTemplate.exchange(URI.create(baseUrl + "/reg"), HttpMethod.GET, reqEntity, String.class);
		userToken = respEntity.getBody();
	}
	
	/**
	 * 获取1分钟K线数据
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@Override
	public List<BarField> getMinutelyData(String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
		return commonGetData("min", unifiedSymbol, startDate, endDate);
	}
	
	/**
	 * 获取15分钟K线数据
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@Override
	public List<BarField> getQuarterlyData(String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
		return commonGetData("quarter", unifiedSymbol, startDate, endDate);
	}
	
	/**
	 * 获取1小时K线数据
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@Override
	public List<BarField> getHourlyData(String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
		return commonGetData("hour", unifiedSymbol, startDate, endDate);
	}
	
	/**
	 * 获取日K线数据
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@Override
	public List<BarField> getDailyData(String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
		return commonGetData("day", unifiedSymbol, startDate, endDate);
	}
	
	public List<String> getTradeDates(String exchange, LocalDate startDate, LocalDate endDate){
		DataSet dataSet = getTradeCalendar(exchange, startDate, endDate);
		List<String> resultList = new LinkedList<>();
		Map<String, Integer> keyIndexMap = new HashMap<>();
		for(int i=0; i<dataSet.getFields().length; i++) {
			keyIndexMap.put(dataSet.getFields()[i], i);
		}
		for(String[] item : dataSet.getItems()) {
			if("1".equals(item[keyIndexMap.get("is_open")])) {
				resultList.add(item[keyIndexMap.get("cal_date")]);
			}
		}
		return resultList;
	}
	
	public List<String> getHolidays(String exchange, LocalDate startDate, LocalDate endDate) {
		DataSet dataSet = getTradeCalendar(exchange, startDate, endDate);
		List<String> resultList = new LinkedList<>();
		Map<String, Integer> keyIndexMap = new HashMap<>();
		for(int i=0; i<dataSet.getFields().length; i++) {
			keyIndexMap.put(dataSet.getFields()[i], i);
		}
		for(String[] item : dataSet.getItems()) {
			if("0".equals(item[keyIndexMap.get("is_open")])) {
				resultList.add(item[keyIndexMap.get("cal_date")]);
			}
		}
		return resultList;
	}
	
	private DataSet getTradeCalendar(String exchange, LocalDate startDate, LocalDate endDate){
		String start = "";
		String end = "";
		if(startDate != null) start = startDate.format(fmt);
		if(endDate != null) end = endDate.format(fmt);
		URI uri = URI.create(String.format("%s/calendar/?exchange=%s&startDate=%s&endDate=%s", baseUrl, exchange, start, end));
		return execute(uri);
	}
	
	private List<BarField> commonGetData(String type, String unifiedSymbol, LocalDate startDate, LocalDate endDate){
		URI uri = URI.create(String.format("%s/data/%s?unifiedSymbol=%s&startDate=%s&endDate=%s", baseUrl, type, unifiedSymbol, startDate.format(fmt), endDate.format(fmt)));
		return convertDataSet(execute(uri));
	}
	
	private DataSet execute(URI uri) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-SECRET", nsSecret);
		headers.add("X-TOKEN", userToken);
		headers.add("X-MACHINE", LocalEnvUtils.getMACAddress());
		HttpEntity<?> reqEntity = new HttpEntity<>(headers);
		try {			
			ResponseEntity<DataSet> respEntity = restTemplate.exchange(uri, HttpMethod.GET, reqEntity, DataSet.class);
			return respEntity.getBody();
		} catch(HttpServerErrorException e) {
			JSONObject entity = JSON.parseObject(e.getResponseBodyAsString());
			throw new IllegalStateException(entity.getString("message"));
		} catch (Exception e) {
			throw new IllegalStateException("数据服务连接异常", e);
		}
	}
	
	private List<BarField> convertDataSet(DataSet dataSet) {
		LinkedList<BarField> resultList = new LinkedList<>();
		Map<String, Integer> fieldIndexMap = new HashMap<>();
		for(int i=0; i<dataSet.getFields().length; i++) {
			fieldIndexMap.put(dataSet.getFields()[i], i);
		}
		for(String[] item : dataSet.getItems()) {
			String tradeDateTime = getValue("trade_time", fieldIndexMap, item, "");
			LocalDateTime dateTime = null;
			String actionDay = "";
			String actionTime = "";
			String tradingDay = "";
			long timestamp = 0;
			
			if(StringUtils.isNotBlank(tradeDateTime)) {
				dateTime = LocalDateTime.parse(tradeDateTime, dtfmt);
				actionDay = dateTime.format(fmt);
				actionTime = dateTime.format(tfmt);
				tradingDay = dtUtil.getTradingDay(dateTime).format(fmt);
				timestamp = dateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
			}
			
			if(StringUtils.isNotBlank(getValue("trade_date", fieldIndexMap, item, ""))) {
				tradingDay = getValue("trade_date", fieldIndexMap, item, "");
				dateTime = LocalDateTime.parse(tradingDay + " 09:00:00", dtfmt2);
				actionDay = dateTime.format(fmt);
				actionTime = dateTime.format(tfmt);
				timestamp = dateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
			}
			
			resultList.addFirst(BarField.newBuilder()
					.setUnifiedSymbol(getValue("ns_code", fieldIndexMap, item, ""))
					.setTradingDay(tradingDay)
					.setActionDay(actionDay)
					.setActionTime(actionTime)
					.setActionTimestamp(timestamp)
					.setHighPrice(Double.parseDouble(getValue("high", fieldIndexMap, item, "0")))
					.setClosePrice(Double.parseDouble(getValue("close", fieldIndexMap, item, "0")))
					.setLowPrice(Double.parseDouble(getValue("low", fieldIndexMap, item, "0")))
					.setOpenPrice(Double.parseDouble(getValue("open", fieldIndexMap, item, "0")))
					.setGatewayId("CTP行情")
					.setOpenInterestDelta(Double.parseDouble(getValue("oi_chg", fieldIndexMap, item, "0")))
					.setOpenInterest(Double.parseDouble(getValue("oi", fieldIndexMap, item, "0")))
					.setVolumeDelta((long) Double.parseDouble(getValue("vol", fieldIndexMap, item, "0")))
					.setTurnoverDelta(Double.parseDouble(getValue("amount", fieldIndexMap, item, "0")))
					.setPreClosePrice(Double.parseDouble(getValue("pre_close", fieldIndexMap, item, "0")))
					.setPreSettlePrice(Double.parseDouble(getValue("pre_settle", fieldIndexMap, item, "0")))
					.setPreOpenInterest(Double.parseDouble(getValue("oi", fieldIndexMap, item, "0")) - Double.parseDouble(getValue("oi_chg", fieldIndexMap, item, "0")))
					.build());
		}
		
		return resultList;
	}
	
	private String getValue(String key, Map<String, Integer> fieldIndexMap, String[] item, String defaultVal) {
		return fieldIndexMap.containsKey(key) ? item[fieldIndexMap.get(key)] : defaultVal;
	}
	
	@Data
	protected static class DataSet {
		
		private String[] fields;
		
		private String[][] items;
		
		private int status;
		
		private String error;
		
		private String message;
	}
}
