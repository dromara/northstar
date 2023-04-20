package org.dromara.northstar.gateway.tiger.time;

import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.gateway.TradeTimeDefinition;
import org.dromara.northstar.gateway.model.PeriodSegment;

/**
 * A股连续交易时段 
 * @author KevinHuangwl
 *
 */
public class CnStockTradeTime implements TradeTimeDefinition{

	@Override
	public List<PeriodSegment> tradeTimeSegments() {
		return List.of(
				new PeriodSegment(LocalTime.of(9, 31), LocalTime.of(11, 30)),
				new PeriodSegment(LocalTime.of(13, 1), LocalTime.of(15, 0)));
	}

}
