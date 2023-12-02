package org.dromara.northstar.common.model.core;

import java.util.regex.Pattern;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

@Builder
public record ContractDefinition(
		/**
		 * 品种类别
		 */
		ProductClassEnum productClass,
		/**
		 * 交易所
		 */
		ExchangeEnum exchange,
		/**
		 * 合约代码特征
		 */
		Pattern symbolPattern,
		/**
		 * 手续费（单位：元）
		 */
		double commissionFee,
		/**
		 * 手续费率
		 */
		double commissionRate,
		/**
		 * 交易时间
		 */
		TradeTimeDefinition tradeTimeDef,
		/**
		 * 名称
		 */
		String name
	
	) {

}
