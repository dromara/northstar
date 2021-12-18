package tech.quantit.northstar.main.persistence.po;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;

/**
 * 网关信息
 * @author KevinHuangwl
 *
 */
@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GatewayPO {

	@Id
	private String gatewayId;
	
	private String description;
	
	private GatewayType gatewayType;
	
	private GatewayUsage gatewayUsage;
	
	private String gatewayAdapterType;
	
	private boolean autoConnect;
	
	private Object settings;

	private String bindedMktGatewayId;
}
