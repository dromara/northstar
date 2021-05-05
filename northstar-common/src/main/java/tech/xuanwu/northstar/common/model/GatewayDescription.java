package tech.xuanwu.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.xuanwu.northstar.common.constant.ConnectionState;
import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.GatewayUsage;

/**
 * 网关连接信息
 * @author KevinHuangwl
 *
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class GatewayDescription {

	private String gatewayId;
	
	private String description;
	
	private GatewayType gatewayType;
	
	private GatewayUsage gatewayUsage;
	
	private String gatewayAdapterType;
	
	private boolean autoConnect;
	
	private Object settings;

	private String relativeGatewayId;
	
	@Builder.Default
	private ConnectionState connectionState = ConnectionState.DISCONNECTED;
	
}
