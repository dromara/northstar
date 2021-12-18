package tech.quantit.northstar.gateway.api.domain;

import tech.quantit.northstar.common.constant.GatewayType;

/**
 * 订阅管理器，负责管理订阅信息
 * @author KevinHuangwl
 *
 */
public interface SubscriptionManager {

	boolean subscribable(NormalContract contract);
	
	GatewayType usedFor();
}
