package org.dromara.northstar.common.model;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.GatewayUsage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
