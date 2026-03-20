package org.dromara.northstar.gateway.mktdata;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;

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
	public List<LocalDate> getHolidays(ChannelType channelType, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<Contract> getAllContracts() {
		return Collections.emptyList();
	}

}
