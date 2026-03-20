package org.dromara.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LineTraderDTO {

	private String positionDir;
	
	private String id;
	
	private Double orderPrice;
	
	private Double stopLossPrice;
	
	private Double takeProfitPrice;
	
	private String accountGatewayId;
	
	private String contractId;
	
	private String contractName;
	
	private int pricePrecision;
	
	private boolean isAnyPrice;
	
	private int volume;
	
	private OverlayVO overlay;
	
	@Builder.Default
	private State state = State.STANDBY;
	
	public static enum State {
		
		STANDBY,
		
		RUNNING,
		
		COMPLETED
	}
}
