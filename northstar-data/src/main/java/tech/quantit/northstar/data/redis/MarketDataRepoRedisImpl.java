package tech.quantit.northstar.data.redis;

import java.time.LocalDate;
import java.util.List;

import tech.quantit.northstar.data.ds.MarketDataRepoDataServiceImpl;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * redis主要读写当天交易日的数据，其余的数据取自数据服务
 * @author KevinHuangwl
 *
 */
public class MarketDataRepoRedisImpl extends MarketDataRepoDataServiceImpl {

	@Override
	public void dropGatewayData(String gatewayId) {
	}

	/**
	 * redis的数据保存结构
	 * key -> hash
	 * key=BarData:GatewayId:TradingDay
	 * value = {
	 * 	unifiedSymbol: [bar, bar, bar]
	 * }
	 * 设置自动过期，过期时间为Bar数据中tradingDay的20:00
	 */
	@Override
	public void insert(BarField bar) {
	}

	/**
	 * 当天的数据查询redis，非当天数据查询数据服务
	 */
	@Override
	public List<BarField> loadBars(String gatewayId, String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
		return super.loadBars(gatewayId, unifiedSymbol, startDate, endDate);
	}
}
