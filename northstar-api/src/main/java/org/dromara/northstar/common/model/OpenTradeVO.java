package org.dromara.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一笔未平仓开仓成交的锚点信息，用于前端按实际成交绘制持仓线
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OpenTradeVO {

	/* 开仓成交时间戳（毫秒），作为持仓线起点时间 */
	private long timestamp;
	/* 开仓成交实际价格，作为持仓线起点价格 */
	private String price;
}
