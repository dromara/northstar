package org.dromara.northstar.gateway.okx;

import java.time.LocalDate;
import java.util.List;

import org.dromara.northstar.data.IMarketDataRepository;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

public class OkxMarketDataRepoImpl implements IMarketDataRepository{

	@Override
	public void insert(BarField bar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<BarField> loadBars(ContractField contract, LocalDate startDate, LocalDate endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BarField> loadDailyBars(ContractField contract, LocalDate startDate, LocalDate endDate) {
		// TODO Auto-generated method stub
		return null;
	}

}
