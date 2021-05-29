package tech.xuanwu.northstar.gateway.api;

import lombok.extern.slf4j.Slf4j;
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

	public GatewayAbstract(GatewaySettingField gatewaySetting) {
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
		return this.logInfo;
	}

}
