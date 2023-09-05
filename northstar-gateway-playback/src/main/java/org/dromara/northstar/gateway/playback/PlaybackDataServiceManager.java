package org.dromara.northstar.gateway.playback;

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
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.IDataServiceManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.utils.LocalEnvUtils;
import org.dromara.northstar.common.utils.MarketDateTimeUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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
public class PlaybackDataServiceManager implements IDataServiceManager {
	
	private String userToken;
	
	private String dummyToken;

	private String baseUrl;
	
	private DateTimeFormatter dtfmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	private DateTimeFormatter dtfmt2 = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
	
	private MarketDateTimeUtil dtUtil;
	
	private RestTemplate restTemplate;
	
	public PlaybackDataServiceManager(String baseUrl, String secret, RestTemplate restTemplate, MarketDateTimeUtil dtUtil) {
		this.baseUrl =  baseUrl;
		this.userToken = secret;
		this.dtUtil = dtUtil;
		this.restTemplate = restTemplate;
	}
	
	@PostConstruct
	@Retryable
	public void register() {
		log.info("采用外部数据源加载历史数据");
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
	@Retryable
	@Cacheable(cacheNames = "bars", key = "'Minutely_' + #contract.getUnifiedSymbol() + '_' + #startDate + '_' + #endDate")
	@Override
	public List<BarField> getMinutelyData(ContractField contract, LocalDate startDate, LocalDate endDate) {
		log.debug("从数据服务加载历史行情1分钟数据：{}，{} -> {}", contract.getUnifiedSymbol(), startDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), endDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		return commonGetData("min", contract, startDate, endDate);
	}
	
	/**
	 * 获取15分钟K线数据
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@Retryable
	@Cacheable(cacheNames = "bars", key = "'Quarterly_' + #contract.getUnifiedSymbol() + '_' + #startDate + '_' + #endDate")
	@Override
	public List<BarField> getQuarterlyData(ContractField contract, LocalDate startDate, LocalDate endDate) {
		log.debug("从数据服务加载历史行情15分钟数据：{}，{} -> {}", contract.getUnifiedSymbol(), startDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), endDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		return commonGetData("quarter", contract, startDate, endDate);
	}
	
	/**
	 * 获取1小时K线数据
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@Retryable
	@Cacheable(cacheNames = "bars", key = "'Hourly_' + #contract.getUnifiedSymbol() + '_' + #startDate + '_' + #endDate")
	@Override
	public List<BarField> getHourlyData(ContractField contract, LocalDate startDate, LocalDate endDate) {
		log.debug("从数据服务加载历史行情1小时数据：{}，{} -> {}", contract.getUnifiedSymbol(), startDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), endDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		return commonGetData("hour", contract, startDate, endDate);
	}
	
	/**
	 * 获取日K线数据
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@Retryable
	@Cacheable(cacheNames = "bars", key = "'Daily_' + #contract.getUnifiedSymbol() + '_' + #startDate + '_' + #endDate")
	@Override
	public List<BarField> getDailyData(ContractField contract, LocalDate startDate, LocalDate endDate) {
		log.debug("从数据服务加载历史行情日线数据：{}，{} -> {}", contract.getUnifiedSymbol(), startDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), endDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		return commonGetData("day", contract, startDate, endDate);
	}
	
	@Retryable
	@Override
	public List<LocalDate> getHolidays(ExchangeEnum exchange, LocalDate startDate, LocalDate endDate) {
		DataSet dataSet = getTradeCalendar(exchange.toString(), startDate, endDate);
		if(Objects.isNull(dataSet) || Objects.isNull(dataSet.getFields())) {
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

	@Retryable
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
			ProductClassEnum productClass = ProductClassEnum.valueOf(unifiedSymbol.split("@")[2]);
			String name = getValue("name", fieldIndexMap, item, "");
			String unitDesc = getValue("quote_unit_desc", fieldIndexMap, item, "1");
			double marginRate = ProductClassEnum.EQUITY == productClass ? 1 : 0.1;
			double priceTick = ProductClassEnum.EQUITY == productClass ? 0.01 : Double.parseDouble(unitDesc.replaceAll("[^\\d\\.]+", ""));
			try {				
				ContractField playbackContract = ContractField.newBuilder()
						.setUnifiedSymbol(unifiedSymbol)
						.setSymbol(symbol)
						.setExchange(exchange)
						.setCurrency(CurrencyEnum.CNY)
						.setContractId(unifiedSymbol + "@" + ChannelType.PLAYBACK.toString())
						.setFullName(name)
						.setName(name)
						.setChannelType(ChannelType.PLAYBACK.toString())
						.setGatewayId(ChannelType.PLAYBACK.toString())
						.setThirdPartyId(symbol + "@" + ChannelType.PLAYBACK.toString())
						.setLastTradeDateOrContractMonth(getValue("delist_date", fieldIndexMap, item, ""))
						.setLongMarginRatio(marginRate)
						.setShortMarginRatio(marginRate)
						.setProductClass(productClass)
						.setMultiplier(Double.parseDouble(getValue("per_unit", fieldIndexMap, item, "1")))
						.setPriceTick(priceTick)
						.build();
				resultList.add(playbackContract);
			} catch(Exception e) {
				log.warn("无效合约数据：{}", JSON.toJSONString(item));
			}
		}
		return resultList;
	}
	
	
	/**
	 * 获取CTP信息
	 */
	@Retryable
	public JSONObject getCtpMetaSettings(String brokerId) {
		URI uri = URI.create(String.format("%s/ctp/settings?brokerId=%s", baseUrl, brokerId));
		return execute(uri, JSONObject.class).getBody();
	}
	
	@Retryable
	@SuppressWarnings("unchecked")
	public List<ExchangeEnum> getUserAvailableExchanges() {
		URI uri = URI.create(String.format("%s/contracts/availableEx", baseUrl));
		return Optional.ofNullable(execute(uri, List.class).getBody()).orElse(Collections.emptyList())
				.stream()
				.map(ex -> ExchangeEnum.valueOf((String)ex))
				.toList();
	}
	
	private DataSet getTradeCalendar(String exchange, LocalDate startDate, LocalDate endDate){
		String start = "";
		String end = "";
		if(startDate != null) start = startDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
		if(endDate != null) end = endDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
		URI uri = URI.create(String.format("%s/calendar/?exchange=%s&startDate=%s&endDate=%s", baseUrl, exchange, start, end));
		return execute(uri, DataSet.class).getBody();
	}
	
	private List<BarField> commonGetData(String type, ContractField contract, LocalDate startDate, LocalDate endDate){
		URI uri = URI.create(String.format("%s/data/%s?unifiedSymbol=%s&startDate=%s&endDate=%s", baseUrl, type, contract.getUnifiedSymbol(), 
				startDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), endDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER)));
		return convertDataSet(execute(uri, DataSet.class).getBody(), contract);
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
			throw new IllegalStateException("第三方数据服务暂时不可用:" + entity.getString("message"));
		} catch (Exception e) {
			throw new IllegalStateException("数据服务连接异常", e);
		}
	}
	
	private List<BarField> convertDataSet(DataSet dataSet, ContractField contract) {
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
				actionDay = dateTime.format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
				actionTime = dateTime.format(DateTimeConstant.T_FORMAT_FORMATTER);
				tradingDay = dtUtil.getTradingDay(dateTime).format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
				timestamp = dateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
			}
			
			if(StringUtils.isNotBlank(getValue("trade_date", fieldIndexMap, item, ""))) {
				tradingDay = getValue("trade_date", fieldIndexMap, item, "");
				dateTime = LocalDateTime.parse(tradingDay + " 09:00:00", dtfmt2);
				actionDay = dateTime.format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
				actionTime = dateTime.format(DateTimeConstant.T_FORMAT_FORMATTER);
				timestamp = dateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
			}
			
			try {				
				double openInterest = 0;
				double openInterestDelta = 0;
				if(contract.getProductClass() == ProductClassEnum.FUTURES) {
					openInterest = Double.parseDouble(getValue("oi", fieldIndexMap, item, "0"));
					openInterestDelta = Double.parseDouble(getValue("oi_chg", fieldIndexMap, item, "0"));
				}
				resultList.addFirst(BarField.newBuilder()
						.setUnifiedSymbol(contract.getUnifiedSymbol())
						.setTradingDay(tradingDay)
						.setActionDay(actionDay)
						.setActionTime(actionTime)
						.setActionTimestamp(timestamp)
						.setHighPrice(normalizeValue(Double.parseDouble(getValue("high", fieldIndexMap, item, "0")), contract.getPriceTick()))
						.setClosePrice(normalizeValue(Double.parseDouble(getValue("close", fieldIndexMap, item, "0")), contract.getPriceTick()))
						.setLowPrice(normalizeValue(Double.parseDouble(getValue("low", fieldIndexMap, item, "0")), contract.getPriceTick()))
						.setOpenPrice(normalizeValue(Double.parseDouble(getValue("open", fieldIndexMap, item, "0")), contract.getPriceTick()))
						.setGatewayId(contract.getGatewayId())
						.setChannelType(contract.getChannelType())
						.setOpenInterestDelta(openInterestDelta)
						.setOpenInterest(openInterest)
						.setVolume((long) Double.parseDouble(getValue("vol", fieldIndexMap, item, "0")))
						.setTurnover(Double.parseDouble(getValue("amount", fieldIndexMap, item, "0")))
						.setPreClosePrice(Double.parseDouble(getValue("pre_close", fieldIndexMap, item, "0")))
						.setPreSettlePrice(Double.parseDouble(getValue("pre_settle", fieldIndexMap, item, "0")))
						.setPreOpenInterest(openInterest - openInterestDelta)
						.build());
			} catch(Exception e) {
				log.warn("无效合约行情数据：{}", JSON.toJSONString(item));
				log.error("", e);
			}
		}
		
		return resultList;
	}

	private double normalizeValue(double val, double priceTick) {
		return (int)(val / priceTick) * priceTick;
	}
	
	private String getValue(String key, Map<String, Integer> fieldIndexMap, String[] item, String defaultVal) {
		return fieldIndexMap.containsKey(key) && Objects.nonNull(item[fieldIndexMap.get(key)]) ? item[fieldIndexMap.get(key)] : defaultVal;
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
