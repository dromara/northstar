package org.dromara.northstar.common.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ModuleAccountRuntimeVO {

	private Double initBalance;
    private Double available;
    private Double marginUtilized;
    private Double balance;
    private Double netPL;
    private Double realizedPL;
    private Double unrealizedPL;
    private Double totalFees;
    private Double maxDrawdown;
    private Double maxDrawdownRate;
    private Double annualizedReturnRate;
    private Double winRate;
    private Double profitLossRatio;
    private List<PositionVO> modulePositions;
    
}
