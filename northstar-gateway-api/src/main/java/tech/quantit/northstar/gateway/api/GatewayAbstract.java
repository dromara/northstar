package tech.quantit.northstar.gateway.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

@Slf4j
public abstract class GatewayAbstract implements Gateway {

	protected String gatewayId;
	protected String gatewayName;
	protected String logInfo;
	protected boolean autoErrorFlag = false;
	protected long lastConnectBeginTimestamp = 0;
	
	protected String gatewayTradingDay;

	protected GatewaySettingField gatewaySetting;
	
	protected FastEventEngine fastEventEngine;
	
	public final Map<String, ContractField> contractMap = new ConcurrentHashMap<>();
	
	public final GlobalMarketRegistry registry;

	protected GatewayAbstract(GatewaySettingField gatewaySetting, GlobalMarketRegistry registry) {
		this.registry = registry;
		this.gatewaySetting = gatewaySetting;
		this.gatewayId = gatewaySetting.getGatewayId();
		this.gatewayName = gatewaySetting.getGatewayName();
		this.logInfo = (gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_MarketData ? "行情" : "交易") + "网关ID-[" + gatewayId + "] [→] ";
		log.info(logInfo + "开始初始化");

	}

	@Override
	public boolean getAuthErrorFlag() {
		return autoErrorFlag;
	}

	@Override
	public GatewaySettingField getGatewaySetting() {
		return gatewaySetting;
	}

	protected String getLogInfo() {
		return logInfo;
	}

	public FastEventEngine getEventEngine() {
		return fastEventEngine;
	}
	
	public void setAuthErrorFlag(boolean flag){
		autoErrorFlag = flag;
	}
}
