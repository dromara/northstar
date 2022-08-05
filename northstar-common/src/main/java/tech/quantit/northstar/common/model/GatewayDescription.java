package tech.quantit.northstar.common.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.constant.ConnectionState;
import tech.quantit.northstar.common.constant.GatewayUsage;

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
	
	private String gatewayType;
	
	private GatewayUsage gatewayUsage;
	
	private String gatewayAdapterType;
	
	private boolean autoConnect;
	
	private Object settings;
	
	private List<String> subscribedContractGroups;

	private String bindedMktGatewayId;
	
	@Builder.Default
	private ConnectionState connectionState = ConnectionState.DISCONNECTED;
	
}
