package tech.xuanwu.northstar.gateway.sim;

import java.util.Optional;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.InvalidProtocolBufferException;

import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.common.model.SimSettings;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.gateway.api.AbstractGatewayFactory;
import tech.xuanwu.northstar.gateway.api.Gateway;
import tech.xuanwu.northstar.gateway.sim.persistence.SimAccountPO;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

public class SimGatewayFactory extends AbstractGatewayFactory{
	
	private SimMarket simMarket;
	
	private ContractManager contractMgr;
	
	private FastEventEngine fastEventEngine;
	
	public SimGatewayFactory(FastEventEngine fastEventEngine, SimMarket simMarket, ContractManager contractMgr) {
		this.simMarket = simMarket;
		this.contractMgr = contractMgr;
		this.fastEventEngine = fastEventEngine;
	}

	@Override
	public Gateway newInstance(GatewayDescription gatewayDescription) {
		String mdGatewayId = gatewayDescription.getBindedMktGatewayId();
		String accGatewayId = gatewayDescription.getGatewayId();
		Optional<SimAccountPO> opt = simMarket.load(accGatewayId);
		SimSettings settings = JSON.toJavaObject((JSON)JSON.toJSON(gatewayDescription.getSettings()), SimSettings.class);
		GatewaySettingField gwSettings = GatewaySettingField.newBuilder()
				.setGatewayId(gatewayDescription.getGatewayId())
				.setGatewayType(GatewayTypeEnum.GTE_Trade)
				.build();
		SimFactory simFactory = new SimFactory(gatewayDescription.getGatewayId(), fastEventEngine, settings.getTicksOfCommission(),
				contractMgr);
		GwAccountHolder accHolder = simFactory.newGwAccountHolder();
		if(opt.isPresent()) {
			try {
				accHolder.convertFrom(opt.get());
			} catch (InvalidProtocolBufferException e) {
				throw new IllegalStateException(e);
			}
		}
		SimGateway gateway = new SimGatewayLocalImpl(fastEventEngine, gwSettings, accHolder);
		simMarket.addGateway(mdGatewayId, gateway);
		return gateway;
	}

}
