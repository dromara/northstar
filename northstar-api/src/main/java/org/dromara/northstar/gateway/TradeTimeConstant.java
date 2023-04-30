package org.dromara.northstar.gateway;

import java.time.LocalTime;

/**
 * 交易时间集
 * @author KevinHuangwl
 *
 */
public interface TradeTimeConstant {

	LocalTime CN_FT_NIGHT_OPENNING = LocalTime.of(21, 0);
	LocalTime CN_FT_DAY_OPENNING1 = LocalTime.of(9, 0);
	LocalTime CN_FT_DAY_OPENNING2 = LocalTime.of(9, 30);

}
