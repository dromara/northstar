package org.dromara.northstar.gateway.contract;

import java.util.Objects;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.Instrument;
import org.dromara.northstar.gateway.mktdata.MinuteBarGenerator;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

/**
 * 网关合约
 * @author KevinHuangwl
 *
 */
@Slf4j
public class GatewayContract implements IContract, TickDataAware{

	private MinuteBarGenerator barGen;
	
	private Contract contract;
	
	private Instrument ins;
	
	private IMarketCenter mktCenter;
	
	public GatewayContract(IMarketCenter mktCenter, FastEventEngine feEngine, Instrument ins) {
		this.ins = ins;
		this.mktCenter = mktCenter;
		this.contract = ins.contract();
		this.barGen = new MinuteBarGenerator(contract, bar -> feEngine.emitEvent(NorthstarEventType.BAR, bar));
	}
	
	@Override
	public IDataSource dataSource() {
		return ins.dataSource();
	}

	@Override
	public boolean subscribe() {
		log.debug("订阅：{}", contract.contractId());
		return mktCenter.getGateway(channelType()).subscribe(contract);
	}

	@Override
	public boolean unsubscribe() {
		log.debug("退订：{}", contract.contractId());
		return mktCenter.getGateway(channelType()).unsubscribe(contract);
	}

	@Override
	public void onTick(Tick tick) {
		barGen.update(tick);
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
		return contract.gateway().gatewayId();
	}

	@Override
	public ChannelType channelType() {
		return ins.channelType();
	}

	@Override
	public void endOfMarket() {
		barGen.forceEndOfBar();
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
