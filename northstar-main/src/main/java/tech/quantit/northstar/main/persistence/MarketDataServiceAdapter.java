package tech.quantit.northstar.main.persistence;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.main.external.DataServiceManager;
import tech.quantit.northstar.main.persistence.po.MinBarDataPO;
import xyz.redtorch.pb.CoreField.BarField;

@Slf4j
@Repository
@ConditionalOnBean(DataServiceManager.class)
public class MarketDataServiceAdapter implements IMarketDataRepository{

	private static final String EMPTY_IMPLEMENTATION_HINT = "采用历史行情数据服务适配器时，不实现该方法";
	
	@Autowired
	private DataServiceManager dsMgr;
	
	@PostConstruct
	protected void init() {
		log.info("加载历史行情服务适配器");
	}
	
	@Override
	public void init(String gatewayId) {
		log.trace(EMPTY_IMPLEMENTATION_HINT);
	}

	@Override
	public void dropGatewayData(String gatewayId) {
		log.trace(EMPTY_IMPLEMENTATION_HINT);
	}

	@Override
	public void insert(MinBarDataPO bar) {
		log.trace(EMPTY_IMPLEMENTATION_HINT);
	}

	@Override
	public void insertMany(List<MinBarDataPO> barList) {
		log.trace(EMPTY_IMPLEMENTATION_HINT);
	}

	@Override
	public List<MinBarDataPO> loadDataByDate(String gatewayId, String unifiedSymbol, String tradeDay) {
		LocalDate tradeDate = LocalDate.parse(tradeDay, DateTimeConstant.D_FORMAT_INT_FORMATTER);
		int i = tradeDate.getDayOfWeek() == DayOfWeek.MONDAY ? 3 : 1;
		List<BarField> dataSet = dsMgr.getMinutelyData(unifiedSymbol, tradeDate.minusDays(i), tradeDate);
		return dataSet.stream()
				.map(bf -> MinBarDataPO.builder()
						.tradingDay(tradeDay)
						.unifiedSymbol(unifiedSymbol)
						.updateTime(bf.getActionTimestamp())
						.gatewayId(gatewayId)
						.barData(bf.toByteArray())
						.ticksData(Collections.EMPTY_LIST)
						.build())
				.toList();
	}

	@Override
	public List<String> findDataAvailableDates(String gatewayId, String unifiedSymbol, boolean isAsc) {
		// 返回最近一年的数据
		LocalDate today = LocalDate.now();
		LocalDate endDate = today;
		if(today.getDayOfWeek() == DayOfWeek.FRIDAY) {
			endDate = today.plusDays(3);
		}
		if(today.getDayOfWeek() == DayOfWeek.SATURDAY) {
			endDate = today.plusDays(2);
		}
		if(today.getDayOfWeek() == DayOfWeek.SUNDAY) {
			endDate = today.plusDays(1);
		}
		LocalDate oneYearAgo = today.minusDays(365);
		List<String> resultList = dsMgr.getTradeDates("SHFE", oneYearAgo, endDate);
		return isAsc ? resultList : Lists.reverse(resultList);
	}

	@Override
	public void clearDataByTime(String gatewayId, long startTime, long endTime) {
		log.trace(EMPTY_IMPLEMENTATION_HINT);
	}

}
