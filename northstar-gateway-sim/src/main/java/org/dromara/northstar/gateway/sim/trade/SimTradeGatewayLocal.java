package org.dromara.northstar.gateway.sim.trade;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.data.ISimAccountRepository;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
public class SimTradeGatewayLocal implements SimTradeGateway{
	
	protected FastEventEngine feEngine;
	
	@Getter
	private boolean connected;
	@Getter
	protected SimGatewayAccount account;
	
	private GatewayDescription gd;
	
	private ConnectionState connState = ConnectionState.DISCONNECTED;
	
	private ISimAccountRepository simAccountRepo;
	
	private Consumer<OrderField> onOrderCallback = order -> {
		account.getPositionManager().onOrder(order);
		feEngine.emitEvent(NorthstarEventType.ORDER, order);
		log.info("订单反馈：{} {} {} {} {}", order.getOrderDate(), order.getUpdateTime(), order.getOriginOrderId(), order.getOrderStatus(), order.getStatusMsg());
	};
	
	private Consumer<Transaction> onTradeCallback = trans -> {
		TradeField trade = trans.tradeField();
		account.onTrade(trade);
		feEngine.emitEvent(NorthstarEventType.TRADE, trade);
		
		log.info("模拟成交：{}，{}，{}，{}手，成交价：{}，订单ID：{}", trade.getContract().getName(), trade.getDirection(), 
				trade.getOffsetFlag(), trade.getVolume(), trade.getPrice(), trade.getOriginOrderId());
		
		simAccountRepo.save(account.getAccountDescription());
	};
	
	private long lastEmitStatus;
	
	private OrderReqManager orderReqMgr =  new OrderReqManager();
	
	public SimTradeGatewayLocal(FastEventEngine feEngine, GatewayDescription gd, SimGatewayAccount account, ISimAccountRepository simAccountRepo) {
		this.feEngine = feEngine;
		this.account = account;
		this.gd = gd;
		this.simAccountRepo = simAccountRepo;
		account.setOrderReqMgr(orderReqMgr);
	}

	@Override
	public void connect() {
		log.debug("[{}] 模拟网关连线", gd.getGatewayId());
		connected = true;
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
		
		for(PositionField pf : account.getPositionManager().positionFields()) {
			feEngine.emitEvent(NorthstarEventType.POSITION, pf);
		}
		
	}

	@Override
	public void disconnect() {
		log.debug("[{}] 模拟网关断开", gd.getGatewayId());
		connected = false;
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
		log.info("[{}] 模拟网关收到下单请求", gd.getGatewayId());
		OrderRequest orderReq = new OrderRequest(account, submitOrderReq, onOrderCallback, onTradeCallback);
		if(orderReq.validate()) {
			orderReqMgr.submitOrder(orderReq);
		}
		return submitOrderReq.getOriginOrderId();
	}

	@Override
	public boolean cancelOrder(CancelOrderReqField cancelOrderReq) {
		if(!isConnected()) {
			throw new IllegalStateException("网关未连线");
		}
		log.info("[{}] 模拟网关收到撤单请求", gd.getGatewayId());
		orderReqMgr.cancelOrder(cancelOrderReq.getOriginOrderId());
		return true;
	}

	@Override
	public int moneyIO(int money) {
		if(money >= 0) {			
			account.onDeposit(money);
		} else {
			account.onWithdraw(Math.abs(money));
		}
		simAccountRepo.save(account.getAccountDescription());
		feEngine.emitEvent(NorthstarEventType.ACCOUNT, account.accountField());
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
		orderReqMgr.onTick(tick);
		account.getPositionManager().onTick(tick);
		if(tick.getActionTimestamp() - lastEmitStatus > 1000) {
			feEngine.emitEvent(NorthstarEventType.ACCOUNT, account.accountField());
			account.getPositionManager().positionFields().forEach(pf -> feEngine.emitEvent(NorthstarEventType.POSITION, pf));
			lastEmitStatus = tick.getActionTimestamp();
		}
	}

}
