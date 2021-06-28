package tech.xuanwu.northstar.factories;

import com.alibaba.fastjson.JSON;

import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.common.model.SimSettings;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.gateway.api.Gateway;
import tech.xuanwu.northstar.gateway.sim.SimFactory;
import tech.xuanwu.northstar.gateway.sim.SimGateway;
import tech.xuanwu.northstar.gateway.sim.SimGatewayLocalImpl;
import tech.xuanwu.northstar.gateway.sim.SimMarket;
import tech.xuanwu.northstar.gateway.sim.persistence.SimAccountRepository;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

public class SimGatewayFactory extends AbstractGatewayFactory{
	
	private SimMarket simMarket;
	
	private ContractManager contractMgr;
	
	private FastEventEngine fastEventEngine;
	
	private SimAccountRepository simAccRepo;
	
	public SimGatewayFactory(FastEventEngine fastEventEngine, SimMarket simMarket, ContractManager contractMgr,
			SimAccountRepository simAccRepo) {
		this.simMarket = simMarket;
		this.contractMgr = contractMgr;
		this.fastEventEngine = fastEventEngine;
		this.simAccRepo = simAccRepo;
	}

	@Override
	public Gateway newInstance(GatewayDescription gatewayDescription) {
		String mdGatewayId = gatewayDescription.getBindedMktGatewayId();
		SimSettings settings = JSON.toJavaObject((JSON)JSON.toJSON(gatewayDescription.getSettings()), SimSettings.class);
		GatewaySettingField gwSettings = GatewaySettingField.newBuilder()
				.setGatewayId(gatewayDescription.getGatewayId())
				.setGatewayType(GatewayTypeEnum.GTE_Trade)
				.build();
		SimFactory simFactory = new SimFactory(gatewayDescription.getGatewayId(), fastEventEngine, settings.getTicksOfCommission(),
				contractMgr.getContractMapByGateway(mdGatewayId));
		SimGateway gateway = new SimGatewayLocalImpl(fastEventEngine, gwSettings, simFactory.newGwAccountHolder());
		simMarket.addGateway(mdGatewayId, gateway);
		return gateway;
	}

}
