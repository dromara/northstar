package org.dromara.northstar.common.model;

import java.util.List;

import org.dromara.northstar.common.model.core.TimeSlot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 合约配置视图对象
 * 用于前端展示，包含系统默认值和自定义值
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Data
public class ContractConfigVO {

	/**
	 * 配置ID
	 */
	private String id;
	
	/**
	 * 合约ID
	 */
	private String contractId;
	
	/**
	 * 统一合约代码
	 */
	private String unifiedSymbol;
	
	/**
	 * 网关ID
	 */
	private String gatewayId;
	
	/**
	 * 渠道类型
	 */
	private String channelType;
	
	/**
	 * 合约代码
	 */
	private String symbol;
	
	/**
	 * 交易所
	 */
	private String exchange;
	
	/**
	 * 产品类型
	 */
	private String productClass;
	
	/**
	 * 合约名称
	 */
	private String name;
	/**
	 * 价格精度
	 */
	private int pricePrecision;
	/**
	 * 交易量精度
	 */
	private int quantityPrecision;
	
	// ========== 系统默认杠杆配置 ==========
	
	/**
	 * 系统默认保证金率
	 */
	private Double defaultMarginRatio;
	
	/**
	 * 系统默认杠杆倍数
	 */
	private Integer defaultLeverage;
	
	// ========== 系统默认手续费配置 ==========
	
	/**
	 * 系统默认手续费类型：FIXED-固定, RATE-比例
	 */
	private String defaultCommissionType;
	
	/**
	 * 系统默认固定手续费（元/手）
	 */
	private Double defaultCommissionFee;
	
	/**
	 * 系统默认手续费率
	 */
	private Double defaultCommissionRate;
	
	// ========== 交易时间配置（系统默认，只读） ==========
	
	/**
	 * 系统默认交易时段列表
	 */
	private List<TimeSlot> defaultTradeTimeSlots;
	
	/**
	 * 系统默认周末是否休市
	 */
	private Boolean defaultWeekendClosure;
	
	/**
	 * 自定义合约配置
	 */
	private ContractConfigDTO customConfig;
	
}
