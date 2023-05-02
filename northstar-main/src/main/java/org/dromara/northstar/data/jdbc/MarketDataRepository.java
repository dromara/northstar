package org.dromara.northstar.data.jdbc;

import java.util.List;

import org.dromara.northstar.data.jdbc.model.BarDO;
import org.springframework.data.repository.CrudRepository;

public interface MarketDataRepository extends CrudRepository<BarDO, Integer> {

	List<BarDO> findByUnifiedSymbolAndTradingDay(String unifiedSymbol, String tradingDay);
}
