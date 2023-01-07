package tech.quantit.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.constant.GatewayUsage;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class GatewayTypeDescription {
	
	private ChannelType type;
	
	public String getName() {
		return type.name();
	}
	
	public GatewayUsage[] getUsage() {
		return type.usage();
	}
	
	public boolean isAdminOnly() {
		return type.adminOnly();
	}
	
	public boolean isAllowDuplication() {
		return type.allowDuplication();
	}
}
