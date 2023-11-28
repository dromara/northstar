package org.dromara.northstar.gateway;

import java.util.List;

import org.dromara.northstar.gateway.model.PeriodSegment;

@Deprecated
public interface TradeTimeDefinition {

	List<PeriodSegment> tradeTimeSegments();
}
