package tech.xuanwu.northstar.common.model;

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
	 * 交易手续费
	 */
	private int fee;
}
