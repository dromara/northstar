package tech.quantit.northstar.common;

import tech.quantit.northstar.common.constant.GatewayUsage;

public interface GatewayType {

	/**
	 * 类别名称
	 * @return
	 */
	String name();
	/**
	 * 网关用途
	 * @return
	 */
	GatewayUsage[] usage();
	/**
	 * 是否使用管理员模式
	 * @return
	 */
	default boolean adminOnly() {
		return false;
	}
	/**
	 * 是否允许有多个行情网关
	 * @return
	 */
	default boolean allowDuplication() {
		return false;
	}
	
}
