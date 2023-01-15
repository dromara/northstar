package tech.quantit.northstar.gateway.api;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;

@Slf4j
public abstract class GatewayAbstract implements Gateway {

	protected String gatewayId;
	protected String logInfo;
	protected boolean autoErrorFlag = false;
	protected long lastConnectBeginTimestamp = 0;
	
	protected String gatewayTradingDay;

	protected GatewayDescription gatewayDescription;
	
	protected FastEventEngine fastEventEngine;
	
	public final IMarketCenter mktCenter;

	protected GatewayAbstract(GatewayDescription gatewayDescription, IMarketCenter mktCenter) {
		this.mktCenter = mktCenter;
		this.gatewayDescription = gatewayDescription;
		this.gatewayId = gatewayDescription.getGatewayId();
		this.logInfo = (gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA ? "行情" : "交易") + "网关ID-[" + gatewayId + "] [→] ";
		log.info(logInfo + "开始初始化");

	}

	@Override
	public boolean getAuthErrorFlag() {
		return autoErrorFlag;
	}
	
	

	@Override
	public GatewayDescription gatewayDescription() {
		return gatewayDescription;
	}

	@Override
	public String gatewayId() {
		return gatewayId;
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
