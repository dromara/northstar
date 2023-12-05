package org.dromara.northstar.common.model.core;

import java.util.Objects;

import org.dromara.northstar.common.constant.ChannelType;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.CombinationTypeEnum;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.OptionsTypeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

@Builder(toBuilder = true)
public record Contract(
		String gatewayId,
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
		boolean tradable,
		ChannelType channelType	// 渠道来源
	) {
	
	public ContractField toContractField() {
		ContractField.Builder builder = ContractField.newBuilder();
		if(gatewayId != null) {
			builder.setGatewayId(gatewayId);
		}
		if(contractId != null) {
			builder.setContractId(contractId);
		}
		if(name != null) {
			builder.setName(name);
		}
		if(fullName != null) {
			builder.setFullName(fullName);
		}
		if(thirdPartyId != null) {
			builder.setThirdPartyId(thirdPartyId);
		}
		if(unifiedSymbol != null) {
			builder.setUnifiedSymbol(unifiedSymbol);
		}
		if(symbol != null) {
			builder.setSymbol(symbol);
		}
		if(exchange != null) {
			builder.setExchange(exchange);
		}
		if(productClass != null) {
			builder.setProductClass(productClass);
		}
		if(currency != null) {
			builder.setCurrency(currency);
		}
		if(underlyingSymbol != null) {
			builder.setUnderlyingSymbol(underlyingSymbol);
		}
		builder.setStrikePrice(strikePrice);
		if(optionsType != null) {
			builder.setOptionsType(optionsType);
		}
		if(lastTradeDateOrContractMonth != null) {
			builder.setLastTradeDateOrContractMonth(lastTradeDateOrContractMonth);
		}
		if(combinationType != null) {
			builder.setCombinationType(combinationType);
		}
		if(channelType != null) {
			builder.setChannelType(channelType.toString());
		}
		builder.setUnderlyingMultiplier(underlyingMultiplier);
		builder.setMaxMarketOrderVolume(maxMarketOrderVolume);
		builder.setMinMarketOrderVolume(minMarketOrderVolume);
		builder.setMaxLimitOrderVolume(maxLimitOrderVolume);
		builder.setMinLimitOrderVolume(minLimitOrderVolume);
		builder.setMultiplier(multiplier);
		builder.setPriceTick(priceTick);
		builder.setLongMarginRatio(longMarginRatio);
		builder.setShortMarginRatio(shortMarginRatio);
		builder.setPricePrecision(pricePrecision);
		builder.setQuantityPrecision(quantityPrecision);

		return builder.build();
	}

	@Override
	public int hashCode() {
		return Objects.hash(contractId, fullName, name, symbol, unifiedSymbol);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Contract other = (Contract) obj;
		return Objects.equals(contractId, other.contractId) && Objects.equals(fullName, other.fullName)
				&& Objects.equals(name, other.name) && Objects.equals(symbol, other.symbol)
				&& Objects.equals(unifiedSymbol, other.unifiedSymbol);
	}
	
}
