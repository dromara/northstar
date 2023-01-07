package tech.quantit.northstar.gateway.api;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.FastEventEngine;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
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
	
	public final IMarketCenter mktCenter;

	protected GatewayAbstract(GatewaySettingField gatewaySetting, IMarketCenter mktCenter) {
		this.mktCenter = mktCenter;
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
