package tech.quantit.northstar.common;

import tech.quantit.northstar.common.constant.GatewayUsage;

public interface GatewayType {

	GatewayUsage[] usage();
	
	boolean adminOnly();
	
	String name();
}
