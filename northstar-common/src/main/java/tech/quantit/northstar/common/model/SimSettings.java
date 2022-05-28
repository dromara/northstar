package tech.quantit.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SimSettings implements GatewaySettings{
	
	/**
	 * 初始账户资金
	 */
	private int initBalance;
}
