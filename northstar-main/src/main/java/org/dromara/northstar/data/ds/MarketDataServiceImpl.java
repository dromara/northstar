package org.dromara.northstar.data.ds;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.dromara.northstar.common.IDataServiceManager;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.data.IMarketDataRepository;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreField.BarField;

@Slf4j
public class MarketDataServiceImpl implements IMarketDataRepository{

	private static final String EMPTY_IMPLEMENTATION_HINT = "采用历史行情数据服务适配器时，不实现该方法";
	
	private IDataServiceManager dsMgr;
	
	public MarketDataServiceImpl(IDataServiceManager dsMgr) {
		this.dsMgr = dsMgr;
	}
	
	@Override
	public void insert(BarField bar) {
		log.trace(EMPTY_IMPLEMENTATION_HINT);
	}

	@Override
	public List<BarField> loadBars(String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
		log.debug("从数据服务加载历史行情分钟数据：{}，{} -> {}", unifiedSymbol, startDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), endDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		try {
			return dsMgr.getMinutelyData(unifiedSymbol, startDate, endDate);
		} catch (Exception e) {
			log.warn("第三方数据服务暂时不可用：{}", e.getMessage(), e);
			return Collections.emptyList();
		}
	}
	
	@Override
	public List<BarField> loadDailyBars(String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
		log.debug("从数据服务加载历史行情日数据：{}，{} -> {}", unifiedSymbol, startDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), endDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		try {
			return dsMgr.getDailyData(unifiedSymbol, startDate, endDate);
		} catch (Exception e) {
			log.warn("第三方数据服务暂时不可用：{}", e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public List<LocalDate> findHodidayInLaw(String gatewayType, int year) {
		List<LocalDate> resultList;
		try {
			resultList = dsMgr.getHolidays(ExchangeEnum.SHFE, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
		} catch (Exception e) {
			log.warn("第三方数据服务暂时不可用：{}", e.getMessage(), e);
			return Collections.emptyList();
		}
		return resultList.stream()
				.filter(date -> date.getDayOfWeek().getValue() < 6)
				.toList();
	}

}
