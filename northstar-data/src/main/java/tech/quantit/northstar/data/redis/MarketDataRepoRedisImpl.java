package tech.quantit.northstar.data.redis;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.data.ds.DataServiceManager;
import tech.quantit.northstar.data.ds.MarketDataRepoDataServiceImpl;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * redis主要读写当天交易日的数据，其余的数据取自数据服务
 * @author KevinHuangwl
 *
 */
@Slf4j
public class MarketDataRepoRedisImpl extends MarketDataRepoDataServiceImpl {

	private RedisTemplate<String, byte[]> redisTemplate;
	
	private static final String KEY_PREFIX = Constants.APP_NAME + "BarData:";
	
	public MarketDataRepoRedisImpl(RedisTemplate<String, byte[]> redisTemplate, DataServiceManager dsMgr) {
		super(dsMgr);
		this.redisTemplate = redisTemplate;
	}
	
	@Override
	public void dropGatewayData(String gatewayId) {
		Set<String> keys = redisTemplate.keys(KEY_PREFIX + gatewayId + "*");
		for(String key : keys) {
			redisTemplate.delete(key);
		}
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
		String key = String.format("%s%s:%s:%s", KEY_PREFIX, bar.getGatewayId(), bar.getTradingDay(), bar.getUnifiedSymbol());
		redisTemplate.boundListOps(key).rightPush(bar.toByteArray());
	}

	/**
	 * 当天的数据查询redis，非当天数据查询数据服务
	 */
	@Override
	public List<BarField> loadBars(String gatewayId, String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
		LocalDate today = LocalDate.now();
		if(today.isAfter(endDate) || today.isEqual(endDate) && LocalTime.now().isAfter(LocalTime.of(20, 0))) {			
			return super.loadBars(gatewayId, unifiedSymbol, startDate, endDate)
					.stream()
					.sorted((a, b) -> a.getActionTimestamp() < b.getActionTimestamp() ? -1 : 1)
					.toList();
		}
		List<BarField> resultList = new LinkedList<>();
		LocalDate date = today;
		while(!date.isAfter(endDate)) {
			resultList.addAll(findBarData(date, gatewayId, unifiedSymbol));
			date = date.plusDays(1);
		}
		return resultList
				.stream()
				.sorted((a,b) -> a.getActionTimestamp() < b.getActionTimestamp() ? -1 : 1)
				.toList();
	}
	
	private List<BarField> findBarData(LocalDate date, String gatewayId, String unifiedSymbol){
		String key = String.format("%s%s:%s:%s", KEY_PREFIX, gatewayId, date.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), unifiedSymbol);
		BoundListOperations<String, byte[]> list = redisTemplate.boundListOps(key);
		return Optional
				.ofNullable(list.range(0, list.size()))
				.orElse(Collections.emptyList())
				.stream()
				.map(this::convert)
				.toList();
	}
	
	private BarField convert(byte[] data) {
		try {
			return BarField.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			log.warn("", e);
			return null;
		}
	}
}
