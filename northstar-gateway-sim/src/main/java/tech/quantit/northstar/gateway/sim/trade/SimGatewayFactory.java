package tech.quantit.northstar.gateway.sim.trade;

import com.google.protobuf.InvalidProtocolBufferException;

import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.SimAccountDescription;
import tech.quantit.northstar.data.ISimAccountRepository;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.IMarketCenter;
import tech.quantit.northstar.gateway.sim.market.SimMarketGatewayLocal;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

public class SimGatewayFactory implements GatewayFactory{
	
	private SimMarket simMarket;
	
	private FastEventEngine fastEventEngine;
	
	private ISimAccountRepository simAccountRepo;
	
	private IMarketCenter mktCenter;
	
	public SimGatewayFactory(FastEventEngine fastEventEngine, SimMarket simMarket, ISimAccountRepository repo, IMarketCenter mktCenter) {
		this.simMarket = simMarket;
		this.fastEventEngine = fastEventEngine;
		this.simAccountRepo = repo;
		this.mktCenter = mktCenter;
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
		return new SimMarketGatewayLocal(gwSettings, fastEventEngine, mktCenter);
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
			account = new SimAccount(accGatewayId, mktCenter, fastEventEngine, simAccDescription -> simAccountRepo.save(simAccDescription));
		} else {
			try {
				account = new SimAccount(simAccountDescription, mktCenter, fastEventEngine, simAccDescription -> simAccountRepo.save(simAccDescription));
			} catch (InvalidProtocolBufferException e) {
				throw new IllegalStateException("无法创建模拟账户", e);
			}
		}
		SimTradeGateway gateway = new SimTradeGatewayLocal(fastEventEngine, simMarket, gwSettings, mdGatewayId, account);
		simMarket.addGateway(mdGatewayId, gateway);
		return gateway;
	}

}
