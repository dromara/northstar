package tech.quantit.northstar.gateway.ctp.time;

import java.time.LocalTime;
import java.util.List;

import tech.quantit.northstar.gateway.api.domain.time.PeriodSegment;
import tech.quantit.northstar.gateway.api.domain.time.TradeTimeDefinition;

/**
 * 国内商品期货三类品种交易时间（有夜盘，2:30收盘）
 * @author KevinHuangwl
 *
 */
public final class CnFtComTradeTime3 implements TradeTimeDefinition {

	@Override
	public List<PeriodSegment> getPeriodSegments() {
		return List.of(
				new PeriodSegment(LocalTime.of(21, 0), LocalTime.of(2, 30)),
				new PeriodSegment(LocalTime.of(9, 1), LocalTime.of(10, 15)),
				new PeriodSegment(LocalTime.of(10, 31), LocalTime.of(11, 30)),
				new PeriodSegment(LocalTime.of(13, 31), LocalTime.of(15, 00))
			);
	}

}
