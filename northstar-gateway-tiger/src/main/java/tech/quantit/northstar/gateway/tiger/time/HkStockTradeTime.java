package tech.quantit.northstar.gateway.tiger.time;

import java.time.LocalTime;
import java.util.List;

import tech.quantit.northstar.gateway.api.domain.time.PeriodSegment;
import tech.quantit.northstar.gateway.api.domain.time.TradeTimeDefinition;

/**
 * 港股连续交易时段 
 * @author KevinHuangwl
 *
 */
public class HkStockTradeTime implements TradeTimeDefinition{

	@Override
	public List<PeriodSegment> tradeTimeSegments() {
		return List.of(
				new PeriodSegment(LocalTime.of(10, 1), LocalTime.of(12, 0)),
				new PeriodSegment(LocalTime.of(13, 1), LocalTime.of(16, 0)));
	}

}
