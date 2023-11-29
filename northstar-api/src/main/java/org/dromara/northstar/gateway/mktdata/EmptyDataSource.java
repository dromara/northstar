package org.dromara.northstar.gateway.mktdata;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;

/**
 * 空数据源，用于占位
 */

public class EmptyDataSource implements IDataSource{

	@Override
	public List<Bar> getMinutelyData(Contract contract, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<Bar> getQuarterlyData(Contract contract, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<Bar> getHourlyData(Contract contract, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<Bar> getDailyData(Contract contract, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<LocalDate> getHolidays(ExchangeEnum exchange, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<Contract> getAllContracts(ExchangeEnum exchange) {
		return Collections.emptyList();
	}

	@Override
	public List<ExchangeEnum> getUserAvailableExchanges() {
		return Collections.emptyList();
	}

}
