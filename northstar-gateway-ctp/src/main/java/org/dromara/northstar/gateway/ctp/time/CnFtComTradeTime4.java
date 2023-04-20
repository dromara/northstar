package org.dromara.northstar.gateway.ctp.time;

import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.gateway.common.domain.time.PeriodSegment;
import org.dromara.northstar.gateway.common.domain.time.TradeTimeDefinition;

/**
 * 国内商品期货四类品种交易时间（无夜盘）
 * @author KevinHuangwl
 *
 */
public final class CnFtComTradeTime4 implements TradeTimeDefinition {

	@Override
	public List<PeriodSegment> tradeTimeSegments() {
		return List.of(
				new PeriodSegment(LocalTime.of(9, 0), LocalTime.of(10, 15)),
				new PeriodSegment(LocalTime.of(10, 31), LocalTime.of(11, 30)),
				new PeriodSegment(LocalTime.of(13, 31), LocalTime.of(15, 00))
			);
	}

}
