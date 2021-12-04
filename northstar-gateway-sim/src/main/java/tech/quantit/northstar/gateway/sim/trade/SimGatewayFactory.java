package tech.quantit.northstar.gateway.sim.trade;

import java.util.Optional;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.InvalidProtocolBufferException;

import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.ContractManager;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.SimSettings;
import tech.quantit.northstar.gateway.api.AbstractGatewayFactory;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.sim.market.SimMarketGatewayLocal;
import tech.quantit.northstar.gateway.sim.persistence.SimAccountPO;
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
		if(gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA) {
			return getMarketGateway(gatewayDescription);
		}
		return getTradeGateway(gatewayDescription);
	}
	
	private Gateway getMarketGateway(GatewayDescription gatewayDescription) {
		GatewaySettingField gwSettings = GatewaySettingField.newBuilder()
				.setGatewayId(gatewayDescription.getGatewayId())
				.setGatewayType(GatewayTypeEnum.GTE_MarketData)
				.build();
		return new SimMarketGatewayLocal(gwSettings, fastEventEngine);
	}
	
	private Gateway getTradeGateway(GatewayDescription gatewayDescription) {
		String mdGatewayId = gatewayDescription.getBindedMktGatewayId();
		String accGatewayId = gatewayDescription.getGatewayId();
		Optional<SimAccountPO> opt = simMarket.load(accGatewayId);
		SimSettings settings = JSON.toJavaObject((JSON)JSON.toJSON(gatewayDescription.getSettings()), SimSettings.class);
		GatewaySettingField gwSettings = GatewaySettingField.newBuilder()
				.setGatewayId(gatewayDescription.getGatewayId())
				.setGatewayType(GatewayTypeEnum.GTE_Trade)
				.build();
		SimFactory simFactory = new SimFactory(gatewayDescription.getGatewayId(), fastEventEngine, settings.getFee(),
				contractMgr);
		GwAccountHolder accHolder = simFactory.newGwAccountHolder();
		if(opt.isPresent()) {
			try {
				accHolder.convertFrom(opt.get());
			} catch (InvalidProtocolBufferException e) {
				throw new IllegalStateException(e);
			}
		}
		SimTradeGateway gateway = new SimTradeGatewayLocal(fastEventEngine, gwSettings, accHolder);
		simMarket.addGateway(mdGatewayId, gateway);
		return gateway;
	}

}
