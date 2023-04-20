package org.dromara.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContractSimpleInfo {

	/**
	 * 合约名
	 */
	private String name;
	/**
	 * 合约代码
	 */
	private String unifiedSymbol;
	/**
	 * 合约ID
	 */
	private String value;
}
