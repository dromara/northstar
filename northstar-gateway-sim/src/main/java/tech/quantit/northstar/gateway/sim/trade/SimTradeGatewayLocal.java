package tech.quantit.northstar.gateway.sim.trade;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.exception.TradeException;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

@Slf4j
public class SimTradeGatewayLocal implements SimTradeGateway{
	
	protected FastEventEngine feEngine;
	
	@Getter
	private GatewaySettingField gatewaySetting;
	@Getter
	private boolean connected;
	@Getter
	protected SimAccount account;
	
	private SimMarket simMarket;
	
	private String bindedMarketGatewayId;
	
	public SimTradeGatewayLocal(FastEventEngine feEngine, SimMarket simMarket, GatewaySettingField gatewaySetting,
			String bindedMarketGatewayId, SimAccount account) {
		this.feEngine = feEngine;
		this.gatewaySetting = gatewaySetting;
		this.bindedMarketGatewayId = bindedMarketGatewayId;
		this.account = account;
		this.simMarket = simMarket;
	}

	@Override
	public void connect() {
		log.debug("[{}] 模拟网关连线", gatewaySetting.getGatewayId());
		connected = true;
		account.setConnected(connected);
		feEngine.emitEvent(NorthstarEventType.CONNECTED, gatewaySetting.getGatewayId());
		feEngine.emitEvent(NorthstarEventType.LOGGED_IN, gatewaySetting.getGatewayId());
		CompletableFuture.runAsync(() -> {
			feEngine.emitEvent(NorthstarEventType.GATEWAY_READY, gatewaySetting.getGatewayId());
		}, CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS));
		// 阻塞一下，防止账户回报比连线回报要快导致异常
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			log.error("", e);
		}
		
		AccountField af = account.accountField();
		feEngine.emitEvent(NorthstarEventType.ACCOUNT, af);
		
		for(PositionField pf : account.positionFields()) {
			feEngine.emitEvent(NorthstarEventType.POSITION, pf);
		}
		
	}

	@Override
	public void disconnect() {
		log.debug("[{}] 模拟网关断开", gatewaySetting.getGatewayId());
		connected = false;
		account.setConnected(connected);
		feEngine.emitEvent(NorthstarEventType.DISCONNECTED, gatewaySetting.getGatewayId());
	}

	@Override
	public String submitOrder(SubmitOrderReqField submitOrderReq) throws TradeException {
		log.debug("[{}] 模拟网关收到下单请求", gatewaySetting.getGatewayId());
		SubmitOrderReqField orderReq = SubmitOrderReqField.newBuilder(submitOrderReq).setGatewayId(gatewaySetting.getGatewayId()).build();
		account.onSubmitOrder(orderReq);
		return orderReq.getOriginOrderId();
	}

	@Override
	public boolean cancelOrder(CancelOrderReqField cancelOrderReq) {
		log.debug("[{}] 模拟网关收到撤单请求", gatewaySetting.getGatewayId());
		account.onCancelOrder(cancelOrderReq);
		return true;
	}

	@Override
	public int moneyIO(int money) {
		if(money >= 0) {			
			account.depositMoney(money);
		} else {
			account.withdrawMoney(Math.abs(money));
		}
		return (int) account.balance();
	}

	@Override
	public boolean getAuthErrorFlag() {
		return false;
	}

	@Override
	public void destory() {
		simMarket.removeGateway(bindedMarketGatewayId, this);
	}

}
