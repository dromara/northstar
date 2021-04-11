package tech.xuanwu.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.xuanwu.northstar.common.constant.GatewayConnectionState;
import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.GatewayUsage;

/**
 * 网关连接信息
 * @author KevinHuangwl
 *
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class GatewayDescription {

	private String gatewayId;
	
	private String description;
	
	private GatewayType gatewayType;
	
	private GatewayUsage gatewayUsage;
	
	private String gatewayAdapterType;
	
	private Object settings;
	
	@Builder.Default
	private GatewayConnectionState connectionState = GatewayConnectionState.DISCONNECTED;
	
	private boolean autoConnect;
	
	private boolean disabled;
	
}
