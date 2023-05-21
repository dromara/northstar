package org.dromara.northstar.gateway.sim;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.dromara.northstar.data.IMarketDataRepository;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

public class DefaultEmptyMarketDataRepo implements IMarketDataRepository{

	@Override
	public void insert(BarField bar) {
		// 不作持久化
	}

	@Override
	public List<BarField> loadBars(ContractField contract, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<BarField> loadDailyBars(ContractField contract, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<LocalDate> findHodidayInLaw(String gatewayType, int year) {
		return Collections.emptyList();
	}

}
