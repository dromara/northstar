package tech.quantit.northstar.gateway.ctp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.gateway.api.domain.contract.ContractDefinition;
import tech.quantit.northstar.gateway.api.domain.contract.Instrument;
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
public class CtpContract implements Instrument{

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
	
	@Override
	public String symbol() {
		return name;
	}

	@Override
	public Identifier indentifier() {
		return new Identifier(unifiedSymbol);
	}

	@Override
	public ContractField mergeToContractField(ContractDefinition contractDef) {
		return ContractField.newBuilder()
				.setContractId(contractId)
				.setName(name)
				.setFullName(fullName)
				.setThirdPartyId(thirdPartyId)
				.setUnifiedSymbol(unifiedSymbol)
				.setSymbol(symbol)
				.setExchange(exchange)
				.setProductClass(productClass)
				.setCurrency(currency)
				.setMultiplier(multiplier)
				.setPriceTick(priceTick)
				.setLongMarginRatio(longMarginRatio)
				.setShortMarginRatio(shortMarginRatio)
				.setMaxMarginSideAlgorithm(maxMarginSideAlgorithm)
				.setUnderlyingSymbol(underlyingSymbol)
				.setStrikePrice(strikePrice)
				.setOptionsType(optionsType)
				.setUnderlyingMultiplier(underlyingMultiplier)
				.setLastTradeDateOrContractMonth(lastTradeDateOrContractMonth)
				.setMaxMarketOrderVolume(maxMarketOrderVolume)
				.setMinMarketOrderVolume(minMarketOrderVolume)
				.setMaxLimitOrderVolume(maxLimitOrderVolume)
				.setMinLimitOrderVolume(minLimitOrderVolume)
				.setCombinationType(combinationType)
				.setGatewayId(gatewayId)
				.setCommissionFee(contractDef.getCommissionFee())
				.setCommissionRate(contractDef.getCommissionRate())
				.build();
	}

}
