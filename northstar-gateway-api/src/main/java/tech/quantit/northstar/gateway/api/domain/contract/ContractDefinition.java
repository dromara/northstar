package tech.quantit.northstar.gateway.api.domain.contract;

import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

@AllArgsConstructor
@Builder
@Data
public class ContractDefinition {
	/**
	 * 网关类别 
	 */
	private String gatewayType;
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
	private double commissionFee;
	/**
	 * 手续费率（万分比）
	 */
	private double commissionRate;
	/**
	 * 交易时间类别
	 */
	private String tradeTimeType;
}
