package org.dromara.northstar.gateway.sim.trade;

import java.util.Objects;
import java.util.Optional;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.Instrument;
import org.dromara.northstar.gateway.TradeTimeDefinition;
import org.dromara.northstar.gateway.model.ContractDefinition;
import org.dromara.northstar.gateway.time.GenericTradeTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import xyz.redtorch.pb.CoreEnum.CombinationTypeEnum;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.OptionsTypeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * CTP合约对象
 * @author KevinHuangwl
 *
 */
@Data
@AllArgsConstructor
@Builder
public class SimContract implements Instrument{

	private String contractId;  					// ID，通常是  <合约代码@交易所代码@产品类型@网关ID>
	private String name;  							// 简称
	private String fullName;  						// 全称
	private String thirdPartyId;  					// 第三方ID
	private String unifiedSymbol;  					// 统一ID，通常是 <合约代码@交易所代码@产品类型>
	private String symbol;  						// 代码
	private ExchangeEnum exchange;  				// 交易所
	private ProductClassEnum productClass;  		// 产品类型
	private CurrencyEnum currency;  				// 币种
	private double multiplier;  					// 合约乘数
	private double priceTick;  						// 最小变动价位
	private double longMarginRatio;  				// 多头保证金率
	private double shortMarginRatio;  				// 空头保证金率
	private boolean maxMarginSideAlgorithm;  		// 最大单边保证金算法
	private String underlyingSymbol;  				// 基础商品代码
	private double strikePrice;  					// 执行价
	private OptionsTypeEnum optionsType;  			// 期权类型
	private double underlyingMultiplier;  			// 合约基础商品乘数
	private String lastTradeDateOrContractMonth;  	// 最后交易日或合约月
	private int maxMarketOrderVolume;  				// 市价单最大下单量
	private int minMarketOrderVolume;  				// 市价单最小下单量
	private int maxLimitOrderVolume;  				// 限价单最大下单量
	private int minLimitOrderVolume;  				// 限价单最小下单量
	private CombinationTypeEnum combinationType; 	// 组合类型
	private String gatewayId;  						// 网关
	
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
	public ContractField contractField() {
		if(Objects.isNull(contractDef)) {
			throw new IllegalStateException("没有合约定义信息");
		}
		return ContractField.newBuilder()
				.setContractId(Optional.ofNullable(contractId).orElse(""))
				.setName(Optional.ofNullable(name).orElse(""))
				.setFullName(Optional.ofNullable(fullName).orElse(""))
				.setThirdPartyId(Optional.ofNullable(thirdPartyId).orElse(""))
				.setUnifiedSymbol(Optional.ofNullable(unifiedSymbol).orElse(""))
				.setSymbol(Optional.ofNullable(symbol).orElse(""))
				.setExchange(Optional.ofNullable(exchange).orElse(ExchangeEnum.UnknownExchange))
				.setProductClass(Optional.ofNullable(productClass).orElse(ProductClassEnum.UnknownProductClass))
				.setCurrency(Optional.ofNullable(currency).orElse(CurrencyEnum.CNY))
				.setMultiplier(multiplier)
				.setPriceTick(priceTick)
				.setLongMarginRatio(longMarginRatio)
				.setShortMarginRatio(shortMarginRatio)
				.setMaxMarginSideAlgorithm(maxMarginSideAlgorithm)
				.setUnderlyingSymbol(Optional.ofNullable(underlyingSymbol).orElse(""))
				.setStrikePrice(strikePrice)
				.setOptionsType(Optional.ofNullable(optionsType).orElse(OptionsTypeEnum.O_Unknown))
				.setUnderlyingMultiplier(underlyingMultiplier)
				.setLastTradeDateOrContractMonth(Optional.ofNullable(lastTradeDateOrContractMonth).orElse(""))
				.setMaxMarketOrderVolume(maxMarketOrderVolume)
				.setMinMarketOrderVolume(minMarketOrderVolume)
				.setMaxLimitOrderVolume(maxLimitOrderVolume)
				.setMinLimitOrderVolume(minLimitOrderVolume)
				.setCombinationType(Optional.ofNullable(combinationType).orElse(CombinationTypeEnum.COMBT_Unknown))
				.setGatewayId(Optional.ofNullable(gatewayId).orElse(""))
				.setChannelType(ChannelType.SIM.toString())
				.setCommissionFee(contractDef.getCommissionFee())
				.setCommissionRate(contractDef.getCommissionRate())
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
	public TradeTimeDefinition tradeTimeDefinition() {
		return new GenericTradeTime();
	}

	@Override
	public ChannelType channelType() {
		return ChannelType.SIM;
	}

}
