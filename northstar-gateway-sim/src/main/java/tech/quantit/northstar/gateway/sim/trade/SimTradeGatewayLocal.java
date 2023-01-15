package tech.quantit.northstar.gateway.sim.trade;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.exception.TradeException;
import tech.quantit.northstar.common.model.GatewayDescription;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

@Slf4j
public class SimTradeGatewayLocal implements SimTradeGateway{
	
	protected FastEventEngine feEngine;
	
	@Getter
	private boolean connected;
	@Getter
	protected SimAccount account;
	
	private SimMarket simMarket;
	
	private GatewayDescription gd;
	
	public SimTradeGatewayLocal(FastEventEngine feEngine, SimMarket simMarket, GatewayDescription gd, SimAccount account) {
		this.feEngine = feEngine;
		this.account = account;
		this.simMarket = simMarket;
		this.gd = gd;
	}

	@Override
	public void connect() {
		log.debug("[{}] 模拟网关连线", gd.getGatewayId());
		connected = true;
		account.setConnected(connected);
		feEngine.emitEvent(NorthstarEventType.CONNECTED, gd.getGatewayId());
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
		feEngine.emitEvent(NorthstarEventType.DISCONNECTED, gd.getGatewayId());
	}

	@Override
	public String submitOrder(SubmitOrderReqField submitOrderReq) throws TradeException {
		log.debug("[{}] 模拟网关收到下单请求", gd.getGatewayId());
		SubmitOrderReqField orderReq = SubmitOrderReqField.newBuilder(submitOrderReq).setGatewayId(gd.getGatewayId()).build();
		account.onSubmitOrder(orderReq);
		return orderReq.getOriginOrderId();
	}

	@Override
	public boolean cancelOrder(CancelOrderReqField cancelOrderReq) {
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
	public void destory() {
		simMarket.removeGateway(gd.getBindedMktGatewayId(), this);
	}

	@Override
	public GatewayDescription gatewayDescription() {
		return gd;
	}

	@Override
	public String gatewayId() {
		return gd.getGatewayId();
	}

}
