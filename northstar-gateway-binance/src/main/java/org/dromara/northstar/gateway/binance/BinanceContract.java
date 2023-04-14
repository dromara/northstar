package org.dromara.northstar.gateway.binance;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.api.domain.contract.ContractDefinition;
import org.dromara.northstar.gateway.api.domain.contract.Instrument;
import org.dromara.northstar.gateway.api.domain.time.GenericTradeTime;
import org.dromara.northstar.gateway.api.domain.time.TradeTimeDefinition;

import com.alibaba.fastjson2.JSONObject;

import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;


public class BinanceContract implements Instrument{

	private JSONObject json;
	
	public BinanceContract(JSONObject json) {
		this.json = json;
	}

	@Override
	public String name() {
		return json.getString("symbol");
	}

	@Override
	public Identifier identifier() {
		return Identifier.of(String.format("%s@%s@%s@%s", name(), exchange(), productClass(), channelType()));
	}

	@Override
	public ProductClassEnum productClass() {
		return ProductClassEnum.FUTURES;
	}

	@Override
	public ExchangeEnum exchange() {
		return ExchangeEnum.UnknownExchange;
	}

	@Override
	public TradeTimeDefinition tradeTimeDefinition() {
		return new GenericTradeTime();
	}

	@Override
	public ChannelType channelType() {
//		return ChannelType.BIAN;
		return null;	// FIXME
	}

	@Override
	public void setContractDefinition(ContractDefinition contractDef) {
	}

	/**
	 * 该合约信息细节还待斟酌
	 */
	@Override
	public ContractField contractField() {
		return ContractField.newBuilder()
				.setGatewayId("BIAN")
				.setSymbol(name())
				.setName(name())
				.setFullName(name())
				.setUnifiedSymbol(String.format("%s@%s@%s", name(), exchange(), productClass()))
				.setCurrency(CurrencyEnum.USD)
				.setExchange(exchange())
				.setProductClass(productClass())
				.setPriceTick(1)
				.setMultiplier(1)
				.setContractId(identifier().value())
				.setLongMarginRatio(json.getDoubleValue("requiredMarginPercent")/100)
				.setShortMarginRatio(json.getDoubleValue("requiredMarginPercent")/100)
				.build();
	}
	
}
