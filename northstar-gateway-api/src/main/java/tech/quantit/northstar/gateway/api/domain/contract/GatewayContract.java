package tech.quantit.northstar.gateway.api.domain.contract;

import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.gateway.api.MarketGateway;
import tech.quantit.northstar.gateway.api.domain.mktdata.MinuteBarGenerator;
import tech.quantit.northstar.gateway.api.domain.time.PeriodHelper;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 网关合约
 * @author KevinHuangwl
 *
 */
public class GatewayContract implements Contract, TickDataAware{

	private MinuteBarGenerator barGen;
	
	private ContractField contract;
	
	private MarketGateway gateway;
	
	private boolean hasSubscribed;
	
	private Instrument ins;
	
	public GatewayContract(FastEventEngine feEngine, Instrument ins, PeriodHelper phHelper) {
		this.gateway = gateway;
		this.ins = ins;
		this.contract = ins.contractField();
		this.barGen = new MinuteBarGenerator(contract, phHelper, bar -> feEngine.emitEvent(NorthstarEventType.BAR, bar));
	}

	@Override
	public boolean subscribe() {
		hasSubscribed = true;
		return gateway.subscribe(contract);
	}

	@Override
	public boolean unsubscribe() {
		hasSubscribed = false;
		return gateway.unsubscribe(contract);
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
	public boolean hasSubscribed() {
		return hasSubscribed;
	}

	@Override
	public String name() {
		return contract.getName();
	}

	@Override
	public Identifier identifier() {
		return ins.identifier();
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
	public String gatewayId() {
		return contract.getGatewayId();
	}

}
