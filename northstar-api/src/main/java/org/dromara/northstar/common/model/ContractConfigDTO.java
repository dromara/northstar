package org.dromara.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 合约配置数据传输对象
 * 同时用于前后端交互和API响应
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Data
public class ContractConfigDTO {

	/**
	 * 合约ID
	 */
	private String contractId;
	
	/**
     * 交易所
     */
    private String exchange;
	
	// ========== 杠杆配置 ==========
	
	/**
	 * 保证金率
	 */
	private Double marginRatio;
	
	/**
	 * 杠杆倍数（计算字段）
	 */
	private Integer leverage;
	
	// ========== 手续费配置 ==========
	
	/**
	 * 手续费类型：FIXED-固定, RATE-比例
	 */
	private String commissionType;
	
	/**
	 * 固定手续费（元/手）
	 */
	private Double commissionFee;
	
	/**
	 * 手续费率
	 */
	private Double commissionRate;
	
}
