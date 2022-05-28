package tech.quantit.northstar.data.redis;

import java.time.LocalDate;
import java.util.List;

import tech.quantit.northstar.data.ds.MarketDataRepoDataServiceImpl;
import xyz.redtorch.pb.CoreField.BarField;

public class MarketDataRepoRedisImpl extends MarketDataRepoDataServiceImpl{

	@Override
	public void dropGatewayData(String gatewayId) {
	}

	@Override
	public void insert(BarField bar) {
	}

	@Override
	public List<BarField> loadBars(String gatewayId, String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
		return super.loadBars(gatewayId, unifiedSymbol, startDate, endDate);
	}
}
