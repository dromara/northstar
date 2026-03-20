package org.dromara.northstar.common.model;

import org.dromara.northstar.common.constant.ConnectionState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class GatewayVO {

	private GatewayDescription description;
	
	private boolean active;
	
	private ConnectionState connection;
}
