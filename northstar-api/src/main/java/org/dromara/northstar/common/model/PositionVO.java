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
    private double total;
    private double available;
    private String openPrice;
    private String lastPrice;
    private double profit;
    private String contractId;
    private DirectionEnum closingDir;
    private String gatewayId;
    private int quantityPrecision;
}
