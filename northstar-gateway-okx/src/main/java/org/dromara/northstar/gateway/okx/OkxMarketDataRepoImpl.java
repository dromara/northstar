package org.dromara.northstar.gateway.okx;

import java.time.LocalDate;
import java.util.List;

import tech.quantit.northstar.gateway.api.IMarketDataRepository;
import xyz.redtorch.pb.CoreField.BarField;

public class OkxMarketDataRepoImpl implements IMarketDataRepository{

	@Override
	public void insert(BarField bar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<BarField> loadBars(String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BarField> loadDailyBars(String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<LocalDate> findHodidayInLaw(String gatewayType, int year) {
		// TODO Auto-generated method stub
		return null;
	}

}
