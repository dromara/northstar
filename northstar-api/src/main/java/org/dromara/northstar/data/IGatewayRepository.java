package org.dromara.northstar.data;

import java.util.List;

import org.dromara.northstar.common.model.GatewayDescription;

/**
 * 网关持久化
 * @author KevinHuangwl
 *
 */
public interface IGatewayRepository {

	/**
	 * 新增网关
	 * @param gatewayDescription
	 */
	void insert(GatewayDescription gatewayDescription);
	/**
	 * 更新网关
	 * @param gatewayDescription
	 */
	void save(GatewayDescription gatewayDescription);
	/**
	 * 删除网关
	 * @param gatewayId
	 */
	void deleteById(String gatewayId);
	/**
	 * 查询网关
	 * @return
	 */
	List<GatewayDescription> findAll();
	/**
	 * 查询网关
	 * @param gatewayId
	 * @return
	 */
	GatewayDescription findById(String gatewayId);
}
