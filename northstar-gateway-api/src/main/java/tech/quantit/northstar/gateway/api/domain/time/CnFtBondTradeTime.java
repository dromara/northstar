package tech.quantit.northstar.gateway.api.domain.time;

import java.time.LocalTime;
import java.util.List;

/**
 * 国内金融期货一类品种交易时间（国债）
 * @author KevinHuangwl
 *
 */
public final class CnFtBondTradeTime implements TradeTimeDefinition {

	@Override
	public List<PeriodSegment> getPeriodSegments() {
		return List.of(
				new PeriodSegment(LocalTime.of(9, 30), LocalTime.of(11, 30)),
				new PeriodSegment(LocalTime.of(13, 1), LocalTime.of(15, 15))
			);
	}

}
