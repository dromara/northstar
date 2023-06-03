package org.dromara.northstar.common.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模组账户配置信息
 * @author KevinHuangwl
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleAccountDescription {

	/**
	 * 账户网关ID
	 */
	private String accountGatewayId;
	/**
	 * 账户关联合约名称
	 */
	private List<ContractSimpleInfo> bindedContracts;
}
