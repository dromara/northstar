package org.dromara.northstar.gateway.sim.trade;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.gateway.Instrument;
import org.dromara.northstar.gateway.mktdata.EmptyDataSource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import xyz.redtorch.pb.CoreEnum.CombinationTypeEnum;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.OptionsTypeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

/**
 * CTP合约对象
 * @author KevinHuangwl
 *
 */
@Data
@AllArgsConstructor
@Builder
public class SimContract implements Instrument{

	private String contractId;                      // ID，通常是  <合约代码@交易所代码@产品类型@网关ID>
	private String name;                              // 简称
	private String fullName;                          // 全称
	private String thirdPartyId;                      // 第三方ID
	private String unifiedSymbol;                      // 统一ID，通常是 <合约代码@交易所代码@产品类型>
	private String symbol;                          // 代码
	private ExchangeEnum exchange;                  // 交易所
	private ProductClassEnum productClass;          // 产品类型
	private CurrencyEnum currency;                  // 币种
	private double multiplier;                      // 合约乘数
	private double priceTick;                          // 最小变动价位
	private double longMarginRatio;                  // 多头保证金率
	private double shortMarginRatio;                  // 空头保证金率
	private boolean maxMarginSideAlgorithm;          // 最大单边保证金算法
	private String underlyingSymbol;                  // 基础商品代码
	private double strikePrice;                      // 执行价
	private OptionsTypeEnum optionsType;              // 期权类型
	private double underlyingMultiplier;              // 合约基础商品乘数
	private LocalDate lastTradeDate;      // 最后交易日或合约月
	private int maxMarketOrderVolume;                  // 市价单最大下单量
	private int minMarketOrderVolume;                  // 市价单最小下单量
	private int maxLimitOrderVolume;                  // 限价单最大下单量
	private int minLimitOrderVolume;                  // 限价单最小下单量
	private CombinationTypeEnum combinationType;     // 组合类型
	private String gatewayId;                          // 网关

	private Identifier identifier;
	private ContractDefinition contractDef;

	@Override
	public String name() {
		return name;
	}

	@Override
	public Identifier identifier() {
		if(Objects.isNull(identifier)) {
			identifier = Identifier.of(contractId);
		}
		return identifier;
	}

	@Override
	public Contract contract() {
		if(Objects.isNull(contractDef)) {
			throw new IllegalStateException("没有合约定义信息");
		}
		return Contract.builder()
				.contractId(Optional.ofNullable(contractId).orElse(""))
				.name(Optional.ofNullable(name).orElse(""))
				.fullName(Optional.ofNullable(fullName).orElse(""))
				.thirdPartyId(Optional.ofNullable(thirdPartyId).orElse(""))
				.unifiedSymbol(Optional.ofNullable(unifiedSymbol).orElse(""))
				.symbol(Optional.ofNullable(symbol).orElse(""))
				.exchange(Optional.ofNullable(exchange).orElse(ExchangeEnum.UnknownExchange))
				.productClass(Optional.ofNullable(productClass).orElse(ProductClassEnum.UnknownProductClass))
				.currency(Optional.ofNullable(currency).orElse(CurrencyEnum.CNY))
				.multiplier(multiplier)
				.priceTick(priceTick)
				.longMarginRatio(longMarginRatio)
				.shortMarginRatio(shortMarginRatio)
				.underlyingSymbol(Optional.ofNullable(underlyingSymbol).orElse(""))
				.strikePrice(strikePrice)
				.optionsType(Optional.ofNullable(optionsType).orElse(OptionsTypeEnum.O_Unknown))
				.underlyingMultiplier(underlyingMultiplier)
				.lastTradeDate(lastTradeDate)
				.maxMarketOrderVolume(maxMarketOrderVolume)
				.minMarketOrderVolume(minMarketOrderVolume)
				.maxLimitOrderVolume(maxLimitOrderVolume)
				.minLimitOrderVolume(minLimitOrderVolume)
				.combinationType(Optional.ofNullable(combinationType).orElse(CombinationTypeEnum.COMBT_Unknown))
				.gatewayId(Optional.ofNullable(gatewayId).orElse(""))
				.channelType(ChannelType.SIM)
				.contractDefinition(contractDef)
				.pricePrecision((int)Math.log10(1 / priceTick))
				.build();
	}

	@Override
	public ProductClassEnum productClass() {
		return productClass;
	}

	@Override
	public ExchangeEnum exchange() {
		return exchange;
	}

	@Override
	public void setContractDefinition(ContractDefinition contractDef) {
		this.contractDef = contractDef;
	}

	@Override
	public ChannelType channelType() {
		return ChannelType.SIM;
	}

	@Override
	public IDataSource dataSource() {
		return new EmptyDataSource();
	}

}
