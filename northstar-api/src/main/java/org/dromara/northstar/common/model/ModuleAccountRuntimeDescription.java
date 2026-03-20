package org.dromara.northstar.common.model;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.PositionField;

/**
 * 模组账户信息
 * @author KevinHuangwl
 *
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleAccountRuntimeDescription {
	
	/**
	 * 初始余额
	 */
	private double initBalance;
	/**
	 * 累计平仓盈亏
	 */
	private double accCloseProfit;
	/**
	 * 累计手续费
	 */
	private double accCommission;
	/**
	 * 模组持仓描述
	 */
	@Builder.Default
	private ModulePositionDescription positionDescription = new ModulePositionDescription();
	/**
	 * 最大盈利
	 */
	private double maxProfit;
	/**
	 * 最大回撤
	 */
	private double maxDrawdown;
	/**
	 * 最大回撤比
	 */
	private double maxDrawdownPercentage;
	/**
	 * 可用余额
	 */
	private double availableAmount;
	
	
	public double unrealizedProfit() {
		return positionDescription.getLogicalPositions().stream()
				.mapToDouble(data -> {
					try {
						return PositionField.parseFrom(data).getPositionProfit();
					} catch (InvalidProtocolBufferException e) {
						log.error("", e);
						return 0;
					}
				})
				.sum();
	}
}
