package tech.quantit.northstar.gateway.api.domain.contract;

import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.gateway.api.MarketGateway;
import tech.quantit.northstar.gateway.api.domain.mktdata.MinuteBarGenerator;
import tech.quantit.northstar.gateway.api.domain.time.IPeriodHelperFactory;
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
	
	protected GatewayContract(MarketGateway gateway, FastEventEngine feEngine, ContractField contract, IPeriodHelperFactory phFactory) {
		this.contract = contract;
		this.gateway = gateway;
		this.barGen = new MinuteBarGenerator(contract, phFactory, bar -> feEngine.emitEvent(NorthstarEventType.BAR, bar));
	}

	@Override
	public boolean subscribe() {
		return gateway.subscribe(contract);
	}

	@Override
	public boolean unsubscribe() {
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
	public String name() {
		return contract.getName();
	}

	@Override
	public Identifier indentifier() {
		return new Identifier(contract.getUnifiedSymbol());
	}

}
