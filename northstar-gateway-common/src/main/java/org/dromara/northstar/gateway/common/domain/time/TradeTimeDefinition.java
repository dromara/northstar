package org.dromara.northstar.gateway.common.domain.time;

import java.util.List;

public interface TradeTimeDefinition {

	List<PeriodSegment> tradeTimeSegments();
}
