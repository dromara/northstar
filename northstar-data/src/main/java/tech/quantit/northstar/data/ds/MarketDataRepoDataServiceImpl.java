package tech.quantit.northstar.data.ds;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.data.IMarketDataRepository;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public class MarketDataRepoDataServiceImpl implements IMarketDataRepository{

	private static final String EMPTY_IMPLEMENTATION_HINT = "采用历史行情数据服务适配器时，不实现该方法";
	
	private DataServiceManager dsMgr;
	
	@Override
	public void init(String gatewayId) {
		log.trace(EMPTY_IMPLEMENTATION_HINT);
	}

	@Override
	public void dropGatewayData(String gatewayId) {
		log.trace(EMPTY_IMPLEMENTATION_HINT);
	}

	@Override
	public void insert(BarField bar) {
		log.trace(EMPTY_IMPLEMENTATION_HINT);
	}

	@Override
	public void insertTicks(List<TickField> tickList) {
		log.trace(EMPTY_IMPLEMENTATION_HINT);
	}

	@Override
	public List<BarField> loadBarsByDate(String gatewayId, String unifiedSymbol, LocalDate tradeDay) {
		int i = tradeDay.getDayOfWeek() == DayOfWeek.MONDAY ? 3 : 1;
		return dsMgr.getMinutelyData(unifiedSymbol, tradeDay.minusDays(i), tradeDay);
	}

	@Override
	public List<TickField> loadTicksByDateTime(String gatewayId, String unifiedSymbol, LocalDateTime dateTime) {
		log.trace(EMPTY_IMPLEMENTATION_HINT);
		return Collections.emptyList();
	}

	@Override
	public List<LocalDate> findAvailableTradeDates(String gatewayId, LocalDate startDate, LocalDate endDate) {
		// 返回最近一年的数据
		LocalDate realEndDate = endDate;
		if(endDate.getDayOfWeek() == DayOfWeek.FRIDAY) {
			realEndDate = endDate.plusDays(3);
		}
		if(endDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
			realEndDate = endDate.plusDays(2);
		}
		if(endDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
			realEndDate = endDate.plusDays(1);
		}
		List<String> resultList = dsMgr.getTradeDates("SHFE", startDate, realEndDate);
		return resultList.stream()
				.map(dateStr -> LocalDate.parse(dateStr, DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.toList();
	}

}
