package org.dromara.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

/**
 * 下单委托
 * @author KevinHuangwl
 *
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderRequestDTO {
	/**
	 * 下单账户ID
	 */
	private String accountId;
	/**
	 * 合约名称
	 */
	private String contractId;
	/**
	 * 委托价
	 */
	private double price;
	/**
	 * 委托数量
	 */
	private int volume;
	/**
	 * 是否市价成交
	 */
	private boolean anyPrice;
	/**
	 * 成交方向
	 */
	private DirectionEnum direction;
	/**
	 * 开平操作
	 */
	private OffsetFlagEnum action;
}
