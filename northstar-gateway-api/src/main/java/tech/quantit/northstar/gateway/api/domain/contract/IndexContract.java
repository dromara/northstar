package tech.quantit.northstar.gateway.api.domain.contract;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.gateway.api.domain.mktdata.IndexTicker;
import tech.quantit.northstar.gateway.api.domain.mktdata.MinuteBarGenerator;
import tech.quantit.northstar.gateway.api.domain.time.IPeriodHelperFactory;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 指数合约
 * @author KevinHuangwl
 *
 */
@Slf4j
public class IndexContract implements Contract, TickDataAware{

	private List<Contract> monthContracts;
	
	private MinuteBarGenerator barGen;
	
	private IndexTicker ticker;
	
	private ContractField contract;
	
	protected IndexContract(FastEventEngine feEngine, ContractField contract, List<Contract> monthContracts, IPeriodHelperFactory phFactory) {
		this.contract = contract;
		this.monthContracts = monthContracts;
		this.barGen = new MinuteBarGenerator(contract, phFactory, bar -> feEngine.emitEvent(NorthstarEventType.BAR, bar));
		this.ticker = new IndexTicker(this, t -> barGen.update(t));
	}

	@Override
	public boolean subscribe() {
		for(Contract c : monthContracts) {
			if(!c.subscribe()) {
				log.warn("[{}] 合约订阅失败", c.contractField().getUnifiedSymbol());
			}
		}
		return true;
	}

	@Override
	public boolean unsubscribe() {
		for(Contract c : monthContracts) {
			if(!c.unsubscribe()) {
				log.warn("[{}] 合约取消订阅失败", c.contractField().getUnifiedSymbol());
			}
		}
		return true;
	}

	@Override
	public void onTick(TickField tick) {
		ticker.update(tick);
	}

	@Override
	public List<Contract> memberContracts() {
		return monthContracts;
	}

	@Override
	public ContractField contractField() {
		return contract;
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
