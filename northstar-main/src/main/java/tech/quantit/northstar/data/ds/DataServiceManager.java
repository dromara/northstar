package tech.quantit.northstar.data.ds;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.utils.LocalEnvUtils;
import tech.quantit.northstar.common.utils.MarketDateTimeUtil;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 历史数据服务接口管理器
 * @author KevinHuangwl
 *
 */

@Slf4j
public class DataServiceManager implements IDataServiceManager {
	
	private String userToken;
	
	private String dummyToken;

	private String baseUrl;
	
	private DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
	
	private DateTimeFormatter dtfmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	private DateTimeFormatter dtfmt2 = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
	
	private DateTimeFormatter tfmt = DateTimeFormatter.ofPattern("HH:mm:ss");
	
	private MarketDateTimeUtil dtUtil;
	
	private RestTemplate restTemplate;
	
	public DataServiceManager(String baseUrl, String secret, RestTemplate restTemplate, MarketDateTimeUtil dtUtil) {
		this.baseUrl =  baseUrl;
		this.userToken = secret;
		this.dtUtil = dtUtil;
		this.restTemplate = restTemplate;
		log.info("采用外部数据源加载历史数据");
		register();
	}
	
	private void register() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-PCNAME", LocalEnvUtils.getPCName());
		headers.add("X-MACHINE", LocalEnvUtils.getMACAddress());
		HttpEntity<?> reqEntity = new HttpEntity<>(headers);
		try {			
			ResponseEntity<String> respEntity = restTemplate.exchange(URI.create(baseUrl + "/reg"), HttpMethod.GET, reqEntity, String.class);
			dummyToken = respEntity.getBody();
		} catch (HttpServerErrorException e) {
			throw new IllegalStateException("无法注册数据服务", e);
		}
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
		Map<Long, BarField> barMap = new HashMap<>();
		return commonGetData("min", unifiedSymbol, startDate, endDate)
				.stream()
				.map(bar -> {
					barMap.put(bar.getActionTimestamp(), bar);
					return bar;
				})
				.filter(bar -> !bar.getActionTime().equals("21:00:00") && !bar.getActionTime().equals("09:00:00"))
				.map(bar -> {
					if(bar.getActionTime().equals("21:01:00") || bar.getActionTime().equals("09:01:00")) {
						BarField lastBar = barMap.get(bar.getActionTimestamp() - 60000);
						if(lastBar != null) {							
							return BarField.newBuilder(bar)
									.setOpenPrice(lastBar.getOpenPrice())
									.setHighPrice(Math.max(lastBar.getOpenPrice(), bar.getHighPrice()))
									.setLowPrice(Math.min(lastBar.getOpenPrice(), bar.getLowPrice()))
									.setOpenInterestDelta(bar.getOpenInterestDelta() + lastBar.getOpenInterestDelta())
									.setVolumeDelta(bar.getVolumeDelta() + lastBar.getVolumeDelta())
									.setTurnoverDelta(bar.getTurnoverDelta() + lastBar.getTurnoverDelta())
									.setNumTradesDelta(bar.getNumTradesDelta() + lastBar.getNumTradesDelta())
									.build();
						}
					}
					return bar;
				})
				.toList();
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
		Map<Long, BarField> barMap = new HashMap<>();
		return commonGetData("quarter", unifiedSymbol, startDate, endDate)
				.stream()
				.map(bar -> {
					barMap.put(bar.getActionTimestamp(), bar);
					return bar;
				})
				.filter(bar -> !bar.getActionTime().equals("21:00:00") && !bar.getActionTime().equals("09:00:00"))
				.map(bar -> {
					if(bar.getActionTime().equals("21:15:00") || bar.getActionTime().equals("09:15:00")) {
						BarField lastBar = barMap.get(bar.getActionTimestamp() - 60000 * 15);
						if(lastBar != null) {							
							return BarField.newBuilder(bar)
									.setOpenPrice(lastBar.getOpenPrice())
									.setOpenInterestDelta(bar.getOpenInterestDelta() + lastBar.getOpenInterestDelta())
									.setVolumeDelta(bar.getVolumeDelta() + lastBar.getVolumeDelta())
									.setTurnoverDelta(bar.getTurnoverDelta() + lastBar.getTurnoverDelta())
									.setNumTradesDelta(bar.getNumTradesDelta() + lastBar.getNumTradesDelta())
									.build();
						}
					}
					return bar;
				})
				.toList();
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
		Map<Long, BarField> barMap = new HashMap<>();
		return commonGetData("hour", unifiedSymbol, startDate, endDate)
				.stream()
				.map(bar -> {
					barMap.put(bar.getActionTimestamp(), bar);
					return bar;
				})
				.filter(bar -> !bar.getActionTime().equals("21:00:00") && !bar.getActionTime().equals("09:00:00"))
				.map(bar -> {
					if(bar.getActionTime().equals("22:00:00") || bar.getActionTime().equals("10:00:00")) {
						BarField lastBar = barMap.get(bar.getActionTimestamp() - 60000 * 60);
						if(lastBar != null) {							
							return BarField.newBuilder(bar)
									.setOpenPrice(lastBar.getOpenPrice())
									.setOpenInterestDelta(bar.getOpenInterestDelta() + lastBar.getOpenInterestDelta())
									.setVolumeDelta(bar.getVolumeDelta() + lastBar.getVolumeDelta())
									.setTurnoverDelta(bar.getTurnoverDelta() + lastBar.getTurnoverDelta())
									.setNumTradesDelta(bar.getNumTradesDelta() + lastBar.getNumTradesDelta())
									.build();
						}
					}
					return bar;
				})
				.toList();
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
	
	@Override
	public List<LocalDate> getHolidays(ExchangeEnum exchange, LocalDate startDate, LocalDate endDate) {
		DataSet dataSet = getTradeCalendar(exchange.toString(), startDate, endDate);
		if(Objects.isNull(dataSet.getFields())) {
			return Collections.emptyList();
		}
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
		return resultList.stream()
				.map(dateStr -> LocalDate.parse(dateStr, DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.toList();
	}

	@Override
	public List<ContractField> getAllContracts(ExchangeEnum exchange) {
		ResponseEntity<DataSet> result = execute(URI.create(String.format("%s/contracts/?exchange=%s", baseUrl, exchange)), DataSet.class);
		DataSet dataSet = result.getBody();
		if(Objects.isNull(dataSet.getFields())) {
			return Collections.emptyList();
		}
		LinkedList<ContractField> resultList = new LinkedList<>();
		Map<String, Integer> fieldIndexMap = new HashMap<>();
		for(int i=0; i<dataSet.getFields().length; i++) {
			fieldIndexMap.put(dataSet.getFields()[i], i);
		}
		for(String[] item : dataSet.getItems()) {
			String unifiedSymbol = getValue("ns_code", fieldIndexMap, item, "");
			String symbol = unifiedSymbol.split("@")[0];
			String name = getValue("name", fieldIndexMap, item, "");
			String unitDesc = getValue("quote_unit_desc", fieldIndexMap, item, "");
			ContractField contract = ContractField.newBuilder()
					.setUnifiedSymbol(unifiedSymbol)
					.setSymbol(symbol)
					.setExchange(exchange)
					.setCurrency(CurrencyEnum.CNY)
					.setContractId(unifiedSymbol + "@CTP")
					.setFullName(name)
					.setName(name)
					.setGatewayId("CTP")
					.setThirdPartyId(symbol + "@CTP")
					.setLastTradeDateOrContractMonth(getValue("delist_date", fieldIndexMap, item, ""))
					.setLongMarginRatio(0.1)
					.setShortMarginRatio(0.1)
					.setProductClass(ProductClassEnum.FUTURES)
					.setMultiplier(Double.parseDouble(getValue("per_unit", fieldIndexMap, item, "0")))
					.setPriceTick(Double.parseDouble(unitDesc.replaceAll("(\\d+\\.?[\\d+]?)[^\\d]+", "$1")))
					.build();
			resultList.add(contract);
		}
		return resultList;
	}
	
	/**
	 * 获取CTP信息
	 */
	@Override
	public JSONObject getCtpMetaSettings(String brokerId) {
		URI uri = URI.create(String.format("%s/ctp/settings?brokerId=%s", baseUrl, brokerId));
		return execute(uri, JSONObject.class).getBody();
	}
	
	private DataSet getTradeCalendar(String exchange, LocalDate startDate, LocalDate endDate){
		String start = "";
		String end = "";
		if(startDate != null) start = startDate.format(fmt);
		if(endDate != null) end = endDate.format(fmt);
		URI uri = URI.create(String.format("%s/calendar/?exchange=%s&startDate=%s&endDate=%s", baseUrl, exchange, start, end));
		return execute(uri, DataSet.class).getBody();
	}
	
	private List<BarField> commonGetData(String type, String unifiedSymbol, LocalDate startDate, LocalDate endDate){
		URI uri = URI.create(String.format("%s/data/%s?unifiedSymbol=%s&startDate=%s&endDate=%s", baseUrl, type, unifiedSymbol, startDate.format(fmt), endDate.format(fmt)));
		return convertDataSet(execute(uri, DataSet.class).getBody());
	}
	
	private <T> ResponseEntity<T> execute(URI uri, Class<T> clz) {
		HttpHeaders headers = new HttpHeaders();
		String token;
		if(StringUtils.isNotBlank(userToken)) {
			token = userToken;
		} else {
			token = dummyToken;
			log.warn("【注意】 当前数据服务调用受限，仅能查询部分基础信息。如需要查询历史行情数据，请向社群咨询。");
		}
		headers.add("Authorization", String.format("Bearer %s", token));
		HttpEntity<?> reqEntity = new HttpEntity<>(headers);
		try {			
			return restTemplate.exchange(uri, HttpMethod.GET, reqEntity, clz);
		} catch (HttpServerErrorException e) {
			JSONObject entity = JSON.parseObject(e.getResponseBodyAsString());
			throw new IllegalStateException(entity.getString("message"));
		} catch (Exception e) {
			throw new IllegalStateException("数据服务连接异常", e);
		}
	}
	
	private List<BarField> convertDataSet(DataSet dataSet) {
		if(Objects.isNull(dataSet.getFields())) {
			log.warn("数据服务查询不到相关数据");
			return Collections.emptyList();
		}
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
					.setVolume((long) Double.parseDouble(getValue("vol", fieldIndexMap, item, "0")))
					.setTurnover(Double.parseDouble(getValue("amount", fieldIndexMap, item, "0")))
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
