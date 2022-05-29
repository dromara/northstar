package tech.quantit.northstar.gateway.sim.trade;

import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.common.ISimAccountRepository;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.SimAccountDescription;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import tech.quantit.northstar.gateway.sim.market.SimMarketGatewayLocal;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

public class SimGatewayFactory implements GatewayFactory{
	
	private SimMarket simMarket;
	
	private FastEventEngine fastEventEngine;
	
	private ISimAccountRepository simAccountRepo;
	
	private GlobalMarketRegistry registry;
	
	private IContractManager contractMgr;
	
	public SimGatewayFactory(FastEventEngine fastEventEngine, SimMarket simMarket, ISimAccountRepository repo, GlobalMarketRegistry registry,
			IContractManager contractMgr) {
		this.simMarket = simMarket;
		this.fastEventEngine = fastEventEngine;
		this.simAccountRepo = repo;
		this.registry = registry;
		this.contractMgr = contractMgr;
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
		return new SimMarketGatewayLocal(gwSettings, fastEventEngine, registry);
	}
	
	private Gateway getTradeGateway(GatewayDescription gatewayDescription) {
		String mdGatewayId = gatewayDescription.getBindedMktGatewayId();
		String accGatewayId = gatewayDescription.getGatewayId();
		SimAccountDescription simAccountDescription = simAccountRepo.findById(accGatewayId);

		GatewaySettingField gwSettings = GatewaySettingField.newBuilder()
				.setGatewayId(gatewayDescription.getGatewayId())
				.setGatewayType(GatewayTypeEnum.GTE_Trade)
				.build();
		
		final SimAccount account;
		if(simAccountDescription == null) {
			account = new SimAccount(accGatewayId, contractMgr);
		} else {
			account = new SimAccount(simAccountDescription, contractMgr);
		}
		account.setEventBus(simMarket.getMarketEventBus());
		account.setFeEngine(fastEventEngine);
		account.setSavingCallback(() -> simAccountRepo.save(account.getDescription()));
		SimTradeGateway gateway = new SimTradeGatewayLocal(fastEventEngine, simMarket, gwSettings, mdGatewayId, account, registry);
		simMarket.addGateway(mdGatewayId, gateway);
		return gateway;
	}

}
