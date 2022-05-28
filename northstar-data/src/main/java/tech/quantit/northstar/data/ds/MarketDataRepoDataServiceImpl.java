package tech.quantit.northstar.data.ds;

import java.time.LocalDate;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.data.IMarketDataRepository;
import xyz.redtorch.pb.CoreField.BarField;

@Slf4j
public class MarketDataRepoDataServiceImpl implements IMarketDataRepository{

	private static final String EMPTY_IMPLEMENTATION_HINT = "采用历史行情数据服务适配器时，不实现该方法";
	
	private DataServiceManager dsMgr;
	
	@Override
	public void dropGatewayData(String gatewayId) {
		log.trace(EMPTY_IMPLEMENTATION_HINT);
	}

	@Override
	public void insert(BarField bar) {
		log.trace(EMPTY_IMPLEMENTATION_HINT);
	}

	@Override
	public List<BarField> loadBars(String gatewayId, String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
		return dsMgr.getMinutelyData(unifiedSymbol, startDate, endDate);
	}

	@Override
	public List<LocalDate> findHodidayInLaw(GatewayType gatewayType, int year) {
		List<String> resultList = dsMgr.getTradeDates("SHFE", LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
		return resultList.stream()
				.map(dateStr -> LocalDate.parse(dateStr, DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.filter(date -> date.getDayOfWeek().getValue() < 6)
				.toList();
	}

}
