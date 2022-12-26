package tech.quantit.northstar.gateway.ctp.time;

import java.time.LocalTime;
import java.util.List;

import tech.quantit.northstar.gateway.api.domain.time.PeriodSegment;
import tech.quantit.northstar.gateway.api.domain.time.TradeTimeDefinition;

/**
 * 国内金融期货二类品种交易时间（股指）
 * @author KevinHuangwl
 *
 */
public final class CnFtIndexTradeTime implements TradeTimeDefinition {

	@Override
	public List<PeriodSegment> getPeriodSegments() {
		return List.of(
				new PeriodSegment(LocalTime.of(9, 30), LocalTime.of(11, 30)),
				new PeriodSegment(LocalTime.of(13, 1), LocalTime.of(15, 00))
			);
	}

}
