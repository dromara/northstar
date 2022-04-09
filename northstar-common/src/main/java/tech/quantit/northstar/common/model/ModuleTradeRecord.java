package tech.quantit.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模组原始成交记录
 * @author KevinHuangwl
 *
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ModuleTradeRecord {
	/**
	 * 模组名称
	 */
	private String moduleName;
	/**
	 * 合约中文名称
	 */
	private String contractName;
	/**
	 * 多空开平操作
	 */
	private String operation;
	/**
	 * 交易日
	 */
	private String tradingDay;
	/**
	 * 操作日
	 */
	private String actionDay;
	/**
	 * 操作时间
	 */
	private long actionTime;
	/**
	 * 手数
	 */
	private int volume;
	/**
	 * 价格
	 */
	private double price;
	
}
