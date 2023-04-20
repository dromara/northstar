package org.dromara.northstar.gateway.common.domain.contract;

import java.util.Objects;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.common.IMarketCenter;
import org.dromara.northstar.gateway.common.domain.mktdata.MinuteBarGenerator;
import org.dromara.northstar.gateway.common.domain.time.TradeTimeDefinition;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 网关合约
 * @author KevinHuangwl
 *
 */
@Slf4j
public class GatewayContract implements Contract, TickDataAware{

	private MinuteBarGenerator barGen;
	
	private ContractField contract;
	
	private Instrument ins;
	
	private IMarketCenter mktCenter;
	
	public GatewayContract(IMarketCenter mktCenter, FastEventEngine feEngine, Instrument ins) {
		this.ins = ins;
		this.mktCenter = mktCenter;
		this.contract = ins.contractField();
		this.barGen = new MinuteBarGenerator(contract, tradeTimeDefinition(), bar -> feEngine.emitEvent(NorthstarEventType.BAR, bar));
	}

	@Override
	public boolean subscribe() {
		log.debug("订阅：{}", contract.getContractId());
		return mktCenter.getGateway(channelType()).subscribe(contract);
	}

	@Override
	public boolean unsubscribe() {
		log.debug("退订：{}", contract.getContractId());
		return mktCenter.getGateway(channelType()).unsubscribe(contract);
	}

	@Override
	public void onTick(TickField tick) {
		barGen.update(tick);
	}

	@Override
	public ContractField contractField() {
		return contract;
	}

	@Override
	public boolean tradable() {
		return true;
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
		return contract.getGatewayId();
	}

	@Override
	public TradeTimeDefinition tradeTimeDefinition() {
		return ins.tradeTimeDefinition();
	}

	@Override
	public ChannelType channelType() {
		return ins.channelType();
	}

	@Override
	public void endOfMarket() {
		barGen.endOfBar();
	}

	@Override
	public int hashCode() {
		return Objects.hash(contract.getContractId());
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
		return Objects.equals(contract.getContractId(), other.contract.getContractId());
	}
	
}
