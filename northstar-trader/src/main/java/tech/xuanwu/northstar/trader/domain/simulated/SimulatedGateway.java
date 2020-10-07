package tech.xuanwu.northstar.trader.domain.simulated;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.constant.GatewayLifecycleEvent;
import tech.xuanwu.northstar.gateway.FastEventEngine;
import tech.xuanwu.northstar.gateway.FastEventEngine.EventType;
import tech.xuanwu.northstar.gateway.GatewayApi;
import tech.xuanwu.northstar.trader.domain.simulated.exception.UnsupportedMethodException;
import xyz.redtorch.pb.CoreEnum.ConnectStatusEnum;
import xyz.redtorch.pb.CoreEnum.GatewayAdapterTypeEnum;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewayField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

/**
 * 模拟网关接口实现，采用真实行情模拟成交
 * @author kevinhuangwl
 *
 */
@Slf4j
public class SimulatedGateway implements GatewayApi{
	
	private FastEventEngine feEngine;
	private volatile GatewayField gw;
	private GatewaySettingField gwSetting;
	
	private boolean connected = false;
	
	private SimulatedMarket simMarket;
	
	public SimulatedGateway(FastEventEngine feEngine, GatewaySettingField gatewaySetting, SimulatedMarket simMarket) {
		this.feEngine = feEngine;
		this.gwSetting = gatewaySetting;
		this.gw = GatewayField.newBuilder()
				.setGatewayId(gatewaySetting.getGatewayId())
				.setName(gatewaySetting.getGatewayName())
				.setGatewayType(GatewayTypeEnum.GTE_Trade)
				.setGatewayAdapterType(GatewayAdapterTypeEnum.GAT_CTP)
				.setStatus(ConnectStatusEnum.CS_Disconnected)
				.build();
		
		this.simMarket = simMarket;
	}
	
	@Override
	public String submitOrder(SubmitOrderReqField submitOrderReq) {
		return simMarket.submitOrderReq(submitOrderReq);
	}
	
	@Override
	public boolean cancelOrder(CancelOrderReqField cancelOrderReq) {
		simMarket.cancelOrder(cancelOrderReq);
		return true;
	}
	
	@Override
	public void connect() {
		log.info("连接模拟网关：{}", gw.getGatewayId());
		connected = true;
		gw = gw.toBuilder()
				.setStatus(ConnectStatusEnum.CS_Connected)
				.build();
		feEngine.emitEvent(EventType.LIFECYCLE, GatewayLifecycleEvent.ON_GATEWAY_CONNECTED, gw.getGatewayId());
	}

	@Override
	public void disconnect() {
		log.info("断开模拟网关：{}", gw.getGatewayId());
		connected = false;
		gw = gw.toBuilder()
				.setStatus(ConnectStatusEnum.CS_Disconnected)
				.build();
		feEngine.emitEvent(EventType.LIFECYCLE, GatewayLifecycleEvent.ON_GATEWAY_DISCONNECTED, gw.getGatewayId());
	}

	@Override
	public boolean isConnected() {
		return connected;
	}
	
	@Override
	public GatewayField getGateway() {
		return gw;
	}

	@Override
	public GatewaySettingField getGatewaySetting() {
		return gwSetting;
	}
	
	@Override
	public boolean subscribe(ContractField contract) {
		throw new UnsupportedMethodException("模拟网关不支持该方法");
	}

	@Override
	public boolean unsubscribe(ContractField contract) {
		throw new UnsupportedMethodException("模拟网关不支持该方法");
	}

	@Override
	public boolean getAuthErrorFlag() {
		throw new UnsupportedMethodException("模拟网关不支持该方法");
	}

	@Override
	public long getLastConnectBeginTimestamp() {
		throw new UnsupportedMethodException("模拟网关不支持该方法");
	}
	
	protected FastEventEngine getEventEngine() {
		return feEngine;
	}
	
	public SimulatedMarket getSimMarket() {
		return simMarket;
	}

}
