package org.dromara.northstar.common.model;

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
    private int total;
    private int available;
    private String openPrice;
    private String lastPrice;
    private int profit;
    private String contractId;
    private DirectionEnum closingDir;
    private String gatewayId;
}
