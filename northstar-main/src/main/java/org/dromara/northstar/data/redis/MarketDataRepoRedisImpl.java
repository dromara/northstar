package org.dromara.northstar.data.redis;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.dromara.northstar.data.ds.DataServiceManager;
import org.dromara.northstar.data.ds.MarketDataRepoDataServiceImpl;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * redis主要读写当天交易日的数据，其余的数据取自数据服务
 * @author KevinHuangwl
 *
 */
@Slf4j
public class MarketDataRepoRedisImpl extends MarketDataRepoDataServiceImpl {

	private RedisTemplate<String, byte[]> redisTemplate;
	
	private static final String KEY_PREFIX = Constants.APP_NAME + "BarData";
	
	public MarketDataRepoRedisImpl(RedisTemplate<String, byte[]> redisTemplate, DataServiceManager dsMgr) {
		super(dsMgr);
		this.redisTemplate = redisTemplate;
	}
	
	/**
	 * redis的数据保存结构
	 * key -> list
	 * key=BarData:GatewayId:TradingDay:unifiedSymbol
	 * value =  [bar, bar, bar]
	 * 设置自动过期，过期时间为Bar数据中tradingDay的20:00
	 */
	@Override
	public void insert(BarField bar) {
		String key = String.format("%s:%s:%s", KEY_PREFIX, bar.getTradingDay(), bar.getUnifiedSymbol());
		redisTemplate.boundListOps(key).rightPush(bar.toByteArray());
		redisTemplate.expireAt(key, LocalDateTime.of(LocalDate.parse(bar.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER), LocalTime.of(20, 0)).toInstant(ZoneOffset.ofHours(8)));
	}

	/**
	 * 当天的数据查询redis，非当天数据查询数据服务
	 */
	@Override
	public List<BarField> loadBars(String unifiedSymbol, LocalDate startDate, LocalDate endDate0) {
		log.debug("加载 [{}] 历史行情数据：{} -> {}", unifiedSymbol, startDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), endDate0.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		LocalDate today = LocalDate.now();
		LocalDate endDate = today.isAfter(endDate0) ? endDate0 : today;
		LinkedList<BarField> resultList = new LinkedList<>();
		if(endDate.isAfter(startDate)) {
			List<BarField> list = super.loadBars(unifiedSymbol, startDate, endDate)
					.stream()
					.sorted((a, b) -> a.getActionTimestamp() < b.getActionTimestamp() ? -1 : 1)
					.toList();
			resultList.addAll(list);
		}
		if(today.isAfter(endDate0))	return resultList; 
		
		LocalDate localQueryDate = today;
		if(resultList.isEmpty()) {
			while(resultList.isEmpty() && endDate0.isAfter(localQueryDate)) {
				resultList.addAll(findBarData(localQueryDate, unifiedSymbol));
				localQueryDate = localQueryDate.plusDays(1);
			}
		} else {			
			if(resultList.peekLast().getTradingDay().equals(today.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
					|| today.getDayOfWeek().getValue() > 5) {
				do {					
					localQueryDate = localQueryDate.plusDays(1);
				} while(localQueryDate.getDayOfWeek().getValue() > 5);
				resultList.addAll(findBarData(localQueryDate, unifiedSymbol));
			} else {
				resultList.addAll(findBarData(localQueryDate, unifiedSymbol));
			}
		}
		
		return resultList;
	}
	
	private List<BarField> findBarData(LocalDate date, String unifiedSymbol){
		log.debug("加载 [{}] 本地行情数据：{}", unifiedSymbol, date.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		String key = String.format("%s:%s:%s", KEY_PREFIX, date.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), unifiedSymbol);
		BoundListOperations<String, byte[]> list = redisTemplate.boundListOps(key);
		return Optional
				.ofNullable(list.range(0, list.size()))
				.orElse(Collections.emptyList())
				.stream()
				.map(this::convert)
				.filter(Objects::nonNull)
				.toList();
	}
	
	private BarField convert(byte[] data) {
		try {
			return BarField.parseFrom(data);
		} catch (Exception e) {
			log.warn("", e);
			return null;
		}
	}
}
