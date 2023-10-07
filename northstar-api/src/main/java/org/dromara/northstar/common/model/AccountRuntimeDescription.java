package org.dromara.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRuntimeDescription {

	/**
	 * 账户名称
	 */
	private String name;
	/**
	 * 账户余额
	 */
	private double balance;
	/**
	 * 账户可用资金
	 */
	private double availableAmount;
	
}
