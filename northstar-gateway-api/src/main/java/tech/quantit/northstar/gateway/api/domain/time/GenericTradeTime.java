package tech.quantit.northstar.gateway.api.domain.time;

import java.time.LocalTime;
import java.util.List;

public class GenericTradeTime implements TradeTimeDefinition {

	@Override
	public List<PeriodSegment> tradeTimeSegments() {
		return List.of(new PeriodSegment(LocalTime.of(0, 1), LocalTime.of(0, 0)));
	}

}
