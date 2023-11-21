package org.dromara.northstar.gateway.mktdata;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.dromara.northstar.common.IDataSource;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 空数据源，用于占位
 */

public class EmptyDataSource implements IDataSource{

	@Override
	public List<BarField> getMinutelyData(ContractField contract, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<BarField> getQuarterlyData(ContractField contract, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<BarField> getHourlyData(ContractField contract, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<BarField> getDailyData(ContractField contract, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<LocalDate> getHolidays(ExchangeEnum exchange, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<ContractField> getAllContracts(ExchangeEnum exchange) {
		return Collections.emptyList();
	}

	@Override
	public List<ExchangeEnum> getUserAvailableExchanges() {
		return Collections.emptyList();
	}

}
