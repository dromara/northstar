package tech.xuanwu.northstar.constant;

public interface GatewayLifecycleEvent {
	
	String BEFORE_GATEWAY_CONNECT = "beforeGatewayConnect";
	
	String ON_GATEWAY_CONNECTED = "onGatewayConnected";
	
	String ON_GATEWAY_READY = "onGatewayReady";
	
	String ON_CTP_CONTRACT_READY = "onCtpContractReady";
	
	String ON_CTP_ACTION_REPLAY_DONE = "onCtpActionReplayDone";
	
	String ON_GATEWAY_DISCONNECTED = "onGatewayDisconnected";
	
}
