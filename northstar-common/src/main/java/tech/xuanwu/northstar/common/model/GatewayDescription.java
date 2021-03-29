package tech.xuanwu.northstar.common.model;

import lombok.Data;
import tech.xuanwu.northstar.common.constant.GatewayConnectionState;
import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.GatewayUsage;

/**
 * 网关连接信息
 * @author KevinHuangwl
 *
 */
@Data
public class GatewayDescription {

	private String gatewayId;
	
	private String description;
	
	private GatewayType gatewayType;
	
	private GatewayUsage gatewayUsage;
	
	private String gatewayAdapterType;
	
	private Object settings;
	
	private GatewayConnectionState connectionState = GatewayConnectionState.DISCONNECTED;
	
	private boolean autoConnect;
	
}
