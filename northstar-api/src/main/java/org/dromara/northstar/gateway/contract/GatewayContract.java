package org.dromara.northstar.gateway.contract;

import java.util.Objects;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.Instrument;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

/**
 * 网关合约
 * @author KevinHuangwl
 *
 */
@Slf4j
public class GatewayContract implements IContract {

	private Contract contract;
	
	private Instrument ins;
	
	public GatewayContract(Instrument ins) {
		this.ins = ins;
		this.contract = ins.contract();
	}
	
	@Override
	public IDataSource dataSource() {
		return ins.dataSource();
	}

	@Override
	public Contract contract() {
		return contract;
	}

	@Override
	public String name() {
		return ins.name();
	}

	@Override
	public Identifier identifier() {
		return ins.identifier();
	}

	@Override
	public ProductClassEnum productClass() {
		return ins.productClass();
	}

	@Override
	public ExchangeEnum exchange() {
		return ins.exchange();
	}

	@Override
	public String gatewayId() {
		return contract.gatewayId();
	}

	@Override
	public ChannelType channelType() {
		return ins.channelType();
	}

	@Override
	public int hashCode() {
		return Objects.hash(contract.contractId());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GatewayContract other = (GatewayContract) obj;
		return Objects.equals(contract.contractId(), other.contract.contractId());
	}
	
}
