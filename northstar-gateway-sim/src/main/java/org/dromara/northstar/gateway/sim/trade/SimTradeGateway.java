package org.dromara.northstar.gateway.sim.trade;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.exception.OperationException;
import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.TradeGateway;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 模拟交易网关
 * 主要负责管理网关状态、消息分发
 */
@Slf4j
public class SimTradeGateway extends TradeGateway implements TransactionAware{
	
	protected FastEventEngine feEngine;
	
	@Getter
	protected SimAccountRemote remoteAccount;
	
	private Executor delayExec = CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS);
	
	private Timer statusReportTimer;
	
	private IMarketCenter mktCenter;
	
	public SimTradeGateway(FastEventEngine feEngine, GatewayDescription gd, SimAccountRemote remoteAccount, IMarketCenter mktCenter) {
		super(gd);
		this.feEngine = feEngine;
		this.remoteAccount = remoteAccount;
		this.mktCenter = mktCenter;
		remoteAccount.setGateway(this);
	}

	@Override
	public void connect() {
		log.debug("[{}] 模拟网关连线", description.getGatewayId());
		connection = ConnectionState.CONNECTED;
		feEngine.emitEvent(NorthstarEventType.LOGGED_IN, description.getGatewayId());
		delayExec.execute(() -> feEngine.emitEvent(NorthstarEventType.GATEWAY_READY, description.getGatewayId()));
		statusReportTimer = new Timer("SimGatewayTimelyReport", true);
		statusReportTimer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				remoteAccount.pollData();
			}
			
		}, 0, 1000);
	}

	@Override
	public void disconnect() {
		log.debug("[{}] 模拟网关断开", description.getGatewayId());
		connection = ConnectionState.DISCONNECTED;
		feEngine.emitEvent(NorthstarEventType.LOGGED_OUT, description.getGatewayId());
		if(statusReportTimer != null) {			
			statusReportTimer.cancel();
		}
	}
	
	@Override
	public void submitOrder(SubmitOrderReq submitOrderReq) throws TradeException {
		if(!isConnected()) {
			throw new OperationException("error.gateway_without_connection", description.getGatewayId());
		}
		log.info("[{}] 模拟网关收到下单请求：{}", description.getGatewayId(), submitOrderReq.originOrderId());
		remoteAccount.submitOrder(submitOrderReq);
	}

	@Override
	public boolean cancelOrder(CancelOrderReq cancelOrderReq) {
		if(!isConnected()) {
			throw new OperationException("error.gateway_without_connection", description.getGatewayId());
		}
		log.info("[{}] 模拟网关收到撤单请求：{}", description.getGatewayId(), cancelOrderReq.originOrderId());
		return remoteAccount.cancelOrder(cancelOrderReq);
	}

	private boolean isConnected() {
		return connection == ConnectionState.CONNECTED;
	}

	public void onTick(Tick tick) {
		if(tick.contract().tradable()) {			
			mktCenter.onTick(tick);
		}
		remoteAccount.onTick(tick);
	}

	public void onAccount(Account account) {
		feEngine.emitEvent(NorthstarEventType.ACCOUNT, account);
	}
	
	public void onPosition(Position position) {
		feEngine.emitEvent(NorthstarEventType.POSITION, position);
	}

	@Override
	public void onOrder(Order order) {
		feEngine.emitEvent(NorthstarEventType.ORDER, order);
	}

	@Override
	public void onTrade(Trade trade) {
		feEngine.emitEvent(NorthstarEventType.TRADE, trade);
	}

}
