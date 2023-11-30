package org.dromara.northstar.gateway.sim.trade;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.data.ISimAccountRepository;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
	
	private Consumer<Order> onOrderCallback = order -> {
		account.getPositionManager().onOrder(order);
		feEngine.emitEvent(NorthstarEventType.ORDER, order);
		log.info("[{}] 订单反馈：{} {} {} {} {}", order.gatewayId(), order.updateDate(), order.updateTime(), order.originOrderId(), order.orderStatus(), order.statusMsg());
	};
	
	private Consumer<Transaction> onTradeCallback = trans -> {
		Trade trade = trans.trade();
		account.onTrade(trade);
		feEngine.emitEvent(NorthstarEventType.TRADE, trade);
		
		log.info("[{}] 模拟成交：{}，{}，{}，{}手，成交价：{}，订单ID：{}", trade.gatewayId(), trade.contract().name(), trade.direction(), 
				trade.offsetFlag(), trade.volume(), trade.price(), trade.originOrderId());
		
		simAccountRepo.save(account.getAccountDescription());
	};
	
	private Timer statusReportTimer;
	
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
		CompletableFuture.runAsync(() -> feEngine.emitEvent(NorthstarEventType.GATEWAY_READY, gd.getGatewayId()),
				CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS));
		statusReportTimer = new Timer("SimGatewayTimelyReport", true);
		statusReportTimer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				feEngine.emitEvent(NorthstarEventType.ACCOUNT, account.account());
				account.getPositionManager().positionFields().forEach(pf -> feEngine.emitEvent(NorthstarEventType.POSITION, pf));
			}
			
		}, 5000, 2000);
	}

	@Override
	public void disconnect() {
		log.debug("[{}] 模拟网关断开", gd.getGatewayId());
		connected = false;
		connState = ConnectionState.DISCONNECTED;
		statusReportTimer.cancel();
	}
	
	@Override
	public ConnectionState getConnectionState() {
		return connState;
	}

	@Override
	public String submitOrder(SubmitOrderReq submitOrderReq) throws TradeException {
		if(!isConnected()) {
			throw new IllegalStateException("网关未连线");
		}
		log.info("[{}] 模拟网关收到下单请求", gd.getGatewayId());
		OrderRequest orderReq = new OrderRequest(account, submitOrderReq, onOrderCallback, onTradeCallback);
		if(orderReq.validate()) {
			orderReqMgr.submitOrder(orderReq);
		}
		return submitOrderReq.originOrderId();
	}

	@Override
	public boolean cancelOrder(String originOrderId) {
		if(!isConnected()) {
			throw new IllegalStateException("网关未连线");
		}
		log.info("[{}] 模拟网关收到撤单请求", gd.getGatewayId());
		orderReqMgr.cancelOrder(originOrderId);
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
		feEngine.emitEvent(NorthstarEventType.ACCOUNT, account.account());
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
	public void onTick(Tick tick) {
		orderReqMgr.onTick(tick);
		account.getPositionManager().onTick(tick);
	}

}
