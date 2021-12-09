package tech.quantit.northstar.gateway.sim.trade;

import java.util.Optional;

import com.alibaba.fastjson.JSON;

import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.SimSettings;
import tech.quantit.northstar.gateway.api.AbstractGatewayFactory;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.sim.market.SimMarketGatewayLocal;
import tech.quantit.northstar.gateway.sim.persistence.SimAccountRepository;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

public class SimGatewayFactory extends AbstractGatewayFactory{
	
	private SimMarket simMarket;
	
	private FastEventEngine fastEventEngine;
	
	private SimAccountRepository simAccountRepo;
	
	public SimGatewayFactory(FastEventEngine fastEventEngine, SimMarket simMarket, SimAccountRepository repo) {
		this.simMarket = simMarket;
		this.fastEventEngine = fastEventEngine;
		this.simAccountRepo = repo;
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
		Optional<SimAccount> simAccountOp = simAccountRepo.findById(accGatewayId);

		SimSettings settings = JSON.toJavaObject((JSON)JSON.toJSON(gatewayDescription.getSettings()), SimSettings.class);
		GatewaySettingField gwSettings = GatewaySettingField.newBuilder()
				.setGatewayId(gatewayDescription.getGatewayId())
				.setGatewayType(GatewayTypeEnum.GTE_Trade)
				.build();
		
		SimAccount account = simAccountOp.orElse(new SimAccount(accGatewayId, settings.getFee()));
		account.setEventBus(simMarket.getMarketEventBus());
		account.setFeEngine(fastEventEngine);
		account.setSavingCallback(() -> simAccountRepo.save(account));
		SimTradeGateway gateway = new SimTradeGatewayLocal(fastEventEngine, gwSettings, account);
		simMarket.addGateway(mdGatewayId, gateway);
		return gateway;
	}

}
