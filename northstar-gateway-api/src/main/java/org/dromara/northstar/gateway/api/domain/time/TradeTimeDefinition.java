package org.dromara.northstar.gateway.api.domain.time;

import java.util.List;

public interface TradeTimeDefinition {

	List<PeriodSegment> tradeTimeSegments();
}
