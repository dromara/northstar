package tech.quantit.northstar.gateway.api.domain.mktdata;

import java.util.List;

import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.gateway.api.IMarketCenter;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;
import tech.quantit.northstar.gateway.api.domain.contract.Instrument;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 市场中心
 * 负责作为网关的防腐层，聚合合约管理以及指数TICK合成
 * @author KevinHuangwl
 *
 */
public class MarketCenter implements IMarketCenter, TickDataAware{

	@Override
	public void addInstrument(Instrument ins) {
		
	}

	@Override
	public void onGatewayReady(String gatewayId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Contract getContract(Identifier identifier) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Contract> getSubscribedContracts(String gatewayId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onTick(TickField tick) {
		// TODO Auto-generated method stub
		
	}

}
