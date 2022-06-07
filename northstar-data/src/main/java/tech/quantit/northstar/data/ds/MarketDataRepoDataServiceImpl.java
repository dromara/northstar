package tech.quantit.northstar.data.ds;

import java.time.LocalDate;
import java.util.Collections;
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
	
	public MarketDataRepoDataServiceImpl(DataServiceManager dsMgr) {
		this.dsMgr = dsMgr;
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
	public List<BarField> loadBars(String gatewayId, String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
		log.debug("从数据服务加载历史行情数据：{}，{} -> {}", unifiedSymbol, startDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), endDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		try {			
			return dsMgr.getMinutelyData(unifiedSymbol, startDate, endDate);
		} catch (Exception e) {
			log.warn("第三方数据服务暂时不可用：{}", e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public List<LocalDate> findHodidayInLaw(GatewayType gatewayType, int year) {
		List<String> resultList;
		try {
			resultList = dsMgr.getHolidays("SHFE", LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
		} catch (Exception e) {
			log.warn("第三方数据服务暂时不可用：{}", e.getMessage(), e);
			return Collections.emptyList();
		}
		return resultList.stream()
				.map(dateStr -> LocalDate.parse(dateStr, DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.filter(date -> date.getDayOfWeek().getValue() < 6)
				.toList();
	}

}
