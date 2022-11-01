package tech.quantit.northstar.strategy.api.utils.time.trade;

import java.util.List;

import tech.quantit.northstar.strategy.api.utils.time.PeriodSegment;

public interface TradeTimeDefinition {

	List<PeriodSegment> getPeriodSegments();
}
