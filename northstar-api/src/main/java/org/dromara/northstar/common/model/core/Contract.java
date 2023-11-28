package org.dromara.northstar.common.model.core;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.model.ContractDefinition;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.CombinationTypeEnum;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.OptionsTypeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

@Builder(toBuilder = true)
public record Contract(
		String contractId,  	// ID，通常是  <合约代码@交易所代码@产品类型@网关ID>
		String name,  			// 简称
		String fullName,  		// 全称
		String thirdPartyId,  	// 第三方ID
		String unifiedSymbol,  	// 统一ID，通常是 <合约代码@交易所代码@产品类型>
		String symbol,  		// 代码
		ExchangeEnum exchange,  // 交易所
		ProductClassEnum productClass,  // 产品类型
		CurrencyEnum currency,  // 币种
		double multiplier,  	// 合约乘数
		double priceTick,  		// 最小变动价位
		double longMarginRatio,  // 多头保证金率
		double shortMarginRatio,  // 空头保证金率
		String underlyingSymbol,  // 基础商品代码
		double strikePrice,  // 执行价
		OptionsTypeEnum optionsType,  // 期权类型
		double underlyingMultiplier,  // 合约基础商品乘数
		String lastTradeDateOrContractMonth,  // 最后交易日或合约月
		int maxMarketOrderVolume,  // 市价单最大下单量
		int minMarketOrderVolume,  // 市价单最小下单量
		int maxLimitOrderVolume,  // 限价单最大下单量
		int minLimitOrderVolume,  // 限价单最小下单量
		CombinationTypeEnum combinationType, // 组合类型
		ContractDefinition contractDefinition,
		int pricePrecision, 	// 价格精度(保留N位小数) 
		int quantityPrecision,	// 成交量精度(保留N位小数)
		Gateway gateway,
		boolean tradable,
		ChannelType channelType	// 渠道来源
	) {

}
