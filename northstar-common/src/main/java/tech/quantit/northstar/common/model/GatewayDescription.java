package tech.quantit.northstar.common.model;

import java.util.Collections;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.constant.ConnectionState;
import tech.quantit.northstar.common.constant.GatewayUsage;

/**
 * 网关连接信息
 * @author KevinHuangwl
 *
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Data
public class GatewayDescription {

	private String gatewayId;
	
	private String description;
	
	private ChannelType channelType;
	
	private GatewayUsage gatewayUsage;
	
	private boolean autoConnect;
	
	private Object settings;
	
	@Builder.Default
	private List<ContractSimpleInfo> subscribedContracts = Collections.emptyList();

	private String bindedMktGatewayId;
	
	@Builder.Default
	private ConnectionState connectionState = ConnectionState.DISCONNECTED;
	
}
