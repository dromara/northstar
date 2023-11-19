package org.dromara.northstar.gateway.playback;

import java.util.Objects;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.Instrument;
import org.dromara.northstar.gateway.TradeTimeDefinition;
import org.dromara.northstar.gateway.model.ContractDefinition;
import org.dromara.northstar.gateway.playback.time.CnFtBondTradeTime;
import org.dromara.northstar.gateway.playback.time.CnFtComTradeTime1;
import org.dromara.northstar.gateway.playback.time.CnFtComTradeTime2;
import org.dromara.northstar.gateway.playback.time.CnFtComTradeTime3;
import org.dromara.northstar.gateway.playback.time.CnFtComTradeTime4;
import org.dromara.northstar.gateway.playback.time.CnFtIndexTradeTime;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

public class PlaybackContract implements Instrument {
	
	private ContractField contract;
	
	private ContractDefinition contractDef;
	
	private IDataSource dataSrc;
	
	public PlaybackContract(ContractField contract, IDataSource dataSrc) {
		this.contract = contract;
		this.dataSrc = dataSrc;
	}

	@Override
	public String name() {
		return contract.getName();
	}

	@Override
	public Identifier identifier() {
		return Identifier.of(contract.getContractId());
	}

	@Override
	public ProductClassEnum productClass() {
		return contract.getProductClass();
	}

	@Override
	public ExchangeEnum exchange() {
		return contract.getExchange();
	}

	@Override
	public TradeTimeDefinition tradeTimeDefinition() {
		if(Objects.isNull(contractDef)) {
			throw new IllegalStateException("没有合约定义信息");
		}
		return switch(contractDef.getTradeTimeType()) {
		case "CN_FT_TT1" -> new CnFtComTradeTime1();
		case "CN_FT_TT2" -> new CnFtComTradeTime2();
		case "CN_FT_TT3" -> new CnFtComTradeTime3();
		case "CN_FT_TT4" -> new CnFtComTradeTime4();
		case "CN_FT_TT5","CN_STK_TT" -> new CnFtIndexTradeTime();
		case "CN_FT_TT6" -> new CnFtBondTradeTime();
		default -> throw new IllegalArgumentException("Unexpected value: " + contractDef.getTradeTimeType());
		};
	}
	
	@Override
	public void setContractDefinition(ContractDefinition contractDef) {
		this.contractDef = contractDef;
	}

	@Override
	public ChannelType channelType() {
		return ChannelType.valueOf(contract.getChannelType());
	}

	@Override
	public ContractField contractField() {
		return ContractField.newBuilder(contract)
					.setCommissionFee(contractDef.getCommissionFee())
					.setCommissionRate(contractDef.getCommissionRate())
					.build();
	}

	@Override
	public IDataSource dataSource() {
		return dataSrc;
	}

}
