package org.dromara.northstar.gateway.sim.trade;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.model.GatewayDescription;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public class SimTradeGatewayLocal implements SimTradeGateway{
	
	protected FastEventEngine feEngine;
	
	@Getter
	private boolean connected;
	@Getter
	protected SimAccount account;
	
	private GatewayDescription gd;
	
	private ConnectionState connState = ConnectionState.DISCONNECTED;
	
	public SimTradeGatewayLocal(FastEventEngine feEngine, GatewayDescription gd, SimAccount account) {
		this.feEngine = feEngine;
		this.account = account;
		this.gd = gd;
	}

	@Override
	public void connect() {
		log.debug("[{}] 模拟网关连线", gd.getGatewayId());
		connected = true;
		account.setConnected(connected);
		connState = ConnectionState.CONNECTED;
		feEngine.emitEvent(NorthstarEventType.LOGGED_IN, gd.getGatewayId());
		CompletableFuture.runAsync(() -> {
			feEngine.emitEvent(NorthstarEventType.GATEWAY_READY, gd.getGatewayId());
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
		log.debug("[{}] 模拟网关断开", gd.getGatewayId());
		connected = false;
		account.setConnected(connected);
		connState = ConnectionState.DISCONNECTED;
	}
	
	@Override
	public ConnectionState getConnectionState() {
		return connState;
	}

	@Override
	public String submitOrder(SubmitOrderReqField submitOrderReq) throws TradeException {
		if(!isConnected()) {
			throw new IllegalStateException("网关未连线");
		}
		log.debug("[{}] 模拟网关收到下单请求", gd.getGatewayId());
		SubmitOrderReqField orderReq = SubmitOrderReqField.newBuilder(submitOrderReq).setGatewayId(gd.getGatewayId()).build();
		account.onSubmitOrder(orderReq);
		return orderReq.getOriginOrderId();
	}

	@Override
	public boolean cancelOrder(CancelOrderReqField cancelOrderReq) {
		if(!isConnected()) {
			throw new IllegalStateException("网关未连线");
		}
		log.debug("[{}] 模拟网关收到撤单请求", gd.getGatewayId());
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
	public GatewayDescription gatewayDescription() {
		gd.setConnectionState(getConnectionState());
		return gd;
	}

	@Override
	public String gatewayId() {
		return gd.getGatewayId();
	}

	@Override
	public void onTick(TickField tick) {
		account.onTick(tick);
	}

}
