package org.dromara.northstar.gateway.playback;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.gateway.Instrument;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

public class PlaybackContract implements Instrument {
	
	private Contract contract;
	
	private ContractDefinition contractDef;
	
	private IDataSource dataSrc;
	
	public PlaybackContract(Contract contract, IDataSource dataSrc) {
		this.contract = contract.toBuilder()
				.gatewayId(ChannelType.PLAYBACK.toString())
				.contractId(contract.unifiedSymbol() + "@" + ChannelType.PLAYBACK)
				.channelType(ChannelType.PLAYBACK)
				.thirdPartyId(contract.symbol() + "@" + ChannelType.PLAYBACK)
				.build();
		this.dataSrc = dataSrc;
	}

	@Override
	public String name() {
		return contract.name();
	}

	@Override
	public Identifier identifier() {
		return Identifier.of(contract.contractId());
	}

	@Override
	public ProductClassEnum productClass() {
		return contract.productClass();
	}

	@Override
	public ExchangeEnum exchange() {
		return contract.exchange();
	}

	@Override
	public void setContractDefinition(ContractDefinition contractDef) {
		this.contractDef = contractDef;
	}

	@Override
	public ChannelType channelType() {
		return contract.channelType();
	}

	@Override
	public Contract contract() {
		return contract.toBuilder()
				.contractDefinition(contractDef)
				.build();
	}

	@Override
	public IDataSource dataSource() {
		return dataSrc;
	}

}
