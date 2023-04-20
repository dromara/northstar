package org.dromara.northstar.gateway.ctp.time;

import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.gateway.common.domain.time.PeriodSegment;
import org.dromara.northstar.gateway.common.domain.time.TradeTimeDefinition;

/**
 * 国内金融期货一类品种交易时间（国债）
 * @author KevinHuangwl
 *
 */
public final class CnFtBondTradeTime implements TradeTimeDefinition {

	@Override
	public List<PeriodSegment> tradeTimeSegments() {
		return List.of(
				new PeriodSegment(LocalTime.of(9, 30), LocalTime.of(11, 30)),
				new PeriodSegment(LocalTime.of(13, 1), LocalTime.of(15, 15))
			);
	}

}
