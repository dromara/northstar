package org.dromara.northstar.common.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PositionVO {

	private String name;
	private String symbol;
    private PositionDirectionEnum dir;
    private double total;
    private double available;
    private String openPrice;
    private String lastPrice;
    private double profit;
    private String contractId;
    private DirectionEnum closingDir;
    private String gatewayId;
    private int quantityPrecision;
    /* 未平仓开仓成交列表（按成交时间升序），持仓可能由多笔成交组成，前端按每笔实际成交绘制持仓线 */
    private List<OpenTradeVO> openTrades;
}
