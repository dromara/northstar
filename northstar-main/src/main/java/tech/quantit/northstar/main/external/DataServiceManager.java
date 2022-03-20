package tech.quantit.northstar.main.external;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.utils.MarketDateTimeUtil;
import xyz.redtorch.gateway.ctp.common.CtpDateTimeUtil;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 历史数据服务接口管理器
 * @author KevinHuangwl
 *
 */

@Slf4j
@Component
@ConditionalOnExpression("#{!'${northstar.data-service.token}'.isBlank()}")
public class DataServiceManager {
	
	@Value("${northstar.data-service.token}")
	private String nsToken;

	@Value("${northstar.data-service.baseUrl}")
	private String baseUrl;
	
	@Autowired
	protected RestTemplate rest;
	
	private DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
	
	private DateTimeFormatter dtfmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	private DateTimeFormatter dtfmt2 = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
	
	private DateTimeFormatter tfmt = DateTimeFormatter.ofPattern("HH:mm:ss");
	
	private MarketDateTimeUtil dtUtil = new CtpDateTimeUtil();
	
	@PostConstruct
	private void init() {
		log.info("采用外部数据源加载历史数据");
	}

	/**
	 * 获取1分钟K线数据
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
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
	public List<BarField> getDailyData(String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
		return commonGetData("day", unifiedSymbol, startDate, endDate);
	}
	
	private List<BarField> commonGetData(String type, String unifiedSymbol, LocalDate startDate, LocalDate endDate){
		URI uri = URI.create(String.format("%s/data/%s?unifiedSymbol=%s&startDate=%s&endDate=%s", baseUrl, type, unifiedSymbol, startDate.format(fmt), endDate.format(fmt)));
		HttpHeaders headers = new HttpHeaders();
		headers.add("token", nsToken);
		HttpEntity<?> reqEntity = new HttpEntity<>(headers);
		ResponseEntity<DataSet> respEntity = rest.exchange(uri, HttpMethod.GET, reqEntity, DataSet.class);
		DataSet dataSet = respEntity.getBody();
		if(dataSet == null) {
			throw new IllegalStateException("历史数据服务返回为空");
		}
		if(respEntity.getStatusCode() != HttpStatus.OK) {
			throw new IllegalStateException("历史数据服务返回异常：" + dataSet.getMessage());
		}
		
		return convertDataSet(dataSet);
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
