package org.dromara.northstar.strategy.tester;

import java.util.List;

import org.dromara.northstar.common.IGatewayService;
import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.TradeGateway;

import com.alibaba.fastjson2.JSONObject;


public class GatewayBuilder {

	private IGatewayService gatewayService;
	
	private ObjectManager<Gateway> gatewayMgr;
	
	private ModuleTesterContext ctx;
	
	public GatewayBuilder(IGatewayService gatewayService, ObjectManager<Gateway> gatewayMgr, ModuleTesterContext ctx) {
		this.gatewayService = gatewayService;
		this.gatewayMgr = gatewayMgr;
		this.ctx = ctx;
	}
	
	public MarketGateway createPlaybackGateway(ContractSimpleInfo csi) {
		String gatewayId = "历史回放_" + csi.getName();
		JSONObject settings = new JSONObject();
		settings.put("preStartDate", ctx.preStartDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		settings.put("startDate", ctx.startDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		settings.put("endDate", ctx.endDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		settings.put("precision", ctx.precision());
		settings.put("speed", ctx.speed());
		settings.put("playContracts", List.of(csi));
		
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId(gatewayId)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.channelType(ChannelType.PLAYBACK)
				.subscribedContracts(List.of(csi))
				.settings(settings)
				.build();
		gatewayService.createGateway(gd);
		return (MarketGateway) gatewayMgr.get(Identifier.of(gatewayId));
	}
	
	public TradeGateway createSimGateway(MarketGateway mktGateway) {
		String gatewayId = "模拟账户_" + mktGateway.gatewayDescription().getSubscribedContracts().get(0).getName();
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId(gatewayId)
				.gatewayUsage(GatewayUsage.TRADE)
				.channelType(ChannelType.SIM)
				.bindedMktGatewayId(mktGateway.gatewayId())
				.settings(new JSONObject())
				.build();
		gatewayService.createGateway(gd);
		return (TradeGateway) gatewayMgr.get(Identifier.of(gatewayId));
	}
}
