package tech.quantit.northstar.common.model;

import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.constant.GatewayType;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

/**
 * 合约定义
 * @author KevinHuangwl
 *
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ContractDefinition {
	/**
	 * 网关类别 
	 */
	private GatewayType gatewayType;
	/**
	 * 品种类别
	 */
	private ProductClassEnum productClass;
	/**
	 * 合约名称
	 */
	private String name;
	/**
	 * 合约代码特征
	 */
	private Pattern symbolPattern;
	/**
	 * 手续费
	 */
	private double commissionInPrice;
	/**
	 * 手续费率（万分比）
	 */
	private double commissionInBasePoint;
	/**
	 * ID 
	 * @return
	 */
	public String contractDefId() {
		return name + "@" + productClass;
	}
}
