package tech.xuanwu.northstar.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.redtorch.pb.CoreEnum.ConnectStatusEnum;
import xyz.redtorch.pb.CoreField.GatewayField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

public abstract class GatewayApiAbstract implements GatewayApi {

	private static Logger log = LoggerFactory.getLogger(GatewayApiAbstract.class);

	protected String gatewayId;
	protected String gatewayName;
	protected String logInfo;
	protected boolean autoErrorFlag = false;
	protected long lastConnectBeginTimestamp = 0;
	
	protected String gatewayTradingDay;

	protected GatewaySettingField gatewaySetting;

	private GatewayField gateway;

	public GatewayApiAbstract(GatewaySettingField gatewaySetting) {
		this.gatewaySetting = gatewaySetting;
		this.gatewayId = gatewaySetting.getGatewayId();
		this.gatewayName = gatewaySetting.getGatewayName();
		this.logInfo = "网关ID-[" + gatewayId + "] 名称-[" + gatewayName + "] [→] ";

		GatewayField.Builder gatewayBuilder = GatewayField.newBuilder();
		gatewayBuilder.setDescription(gatewaySetting.getGatewayDescription());
		gatewayBuilder.setGatewayAdapterType(gatewaySetting.getGatewayAdapterType());
		gatewayBuilder.setGatewayType(gatewaySetting.getGatewayType());
		gatewayBuilder.setGatewayId(gatewaySetting.getGatewayId());
		gatewayBuilder.setName(gatewaySetting.getGatewayName());
		gatewayBuilder.setStatus(ConnectStatusEnum.CS_Disconnected);
		gateway = gatewayBuilder.build();
		log.info(logInfo + "开始初始化");

	}

	@Override
	public boolean getAuthErrorFlag() {
		return autoErrorFlag;
	}
	
	@Override
	public long getLastConnectBeginTimestamp() {
		return this.lastConnectBeginTimestamp;
	}

	@Override
	public GatewaySettingField getGatewaySetting() {
		return gatewaySetting;
	}

	protected String getLogInfo() {
		return this.logInfo;
	}

	@Override
	public GatewayField getGateway() {
		return gateway;
	}
	
}
