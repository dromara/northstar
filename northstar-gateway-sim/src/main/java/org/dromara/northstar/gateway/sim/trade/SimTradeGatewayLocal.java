package org.dromara.northstar.gateway.sim.trade;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.exception.IllegalOrderRequestException;
import org.dromara.northstar.common.exception.OperationException;
import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.TradeGateway;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimTradeGatewayLocal extends TradeGateway{
	
	protected FastEventEngine feEngine;
	
	@Getter
	protected SimGatewayAccount account;
	
	private ISimAccountRepository simAccountRepo;
	
	private Executor delayExec = CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS);
	
	private Consumer<Order> onOrderCallback = order -> {
		account.getPositionManager().onOrder(order);
		feEngine.emitEvent(NorthstarEventType.ORDER, order);
		log.info("[{}] 订单反馈：{} {} {} {} {}", order.gatewayId(), order.updateDate(), order.updateTime(), order.originOrderId(), order.orderStatus(), order.statusMsg());
	};
	
	private Consumer<Transaction> onTradeCallback = trans -> {
		Trade trade = trans.trade();
		account.onTrade(trade);
		feEngine.emitEvent(NorthstarEventType.TRADE, trade);
		feEngine.emitEvent(NorthstarEventType.ACCOUNT, account.account());
		account.getPositionManager().positionFields().forEach(pf -> feEngine.emitEvent(NorthstarEventType.POSITION, pf));
		log.info("[{}] 模拟成交：{}，{}，{}，{}手，成交价：{}，订单ID：{}", trade.gatewayId(), trade.contract().name(), trade.direction(),
				trade.offsetFlag(), trade.volume(), trade.price(), trade.originOrderId());
		
		simAccountRepo.save(account.getAccountDescription());
	};
	
	private Timer statusReportTimer;
	
	private IMarketCenter mktCenter;
	
	public SimTradeGatewayLocal(FastEventEngine feEngine, GatewayDescription gd, SimGatewayAccount account, ISimAccountRepository simAccountRepo,
			IMarketCenter mktCenter) {
		super(gd);
		this.feEngine = feEngine;
		this.account = account;
		this.mktCenter = mktCenter;
		this.simAccountRepo = simAccountRepo;
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
				log.trace("账户 [{}] 定时反馈", description.getGatewayId());
				try {					
					feEngine.emitEvent(NorthstarEventType.ACCOUNT, account.account());
					account.getPositionManager().positionFields().forEach(pf -> feEngine.emitEvent(NorthstarEventType.POSITION, pf));
				} catch(Exception e) {
					log.error("账户信息异常", e);
				}
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
	public String submitOrder(SubmitOrderReq submitOrderReq) throws TradeException {
		if(!isConnected()) {
			throw new OperationException("error.gateway_without_connection", description.getGatewayId());
		}
		log.info("[{}] 模拟网关收到下单请求", description.getGatewayId());
		OrderRequest orderReq = new OrderRequest(account, submitOrderReq, onOrderCallback, onTradeCallback);
		if(orderReq.validate()) {
			account.submitOrder(orderReq);
		} else {
			throw new IllegalOrderRequestException();
		}
		return submitOrderReq.originOrderId();
	}

	@Override
	public boolean cancelOrder(String originOrderId) {
		if(!isConnected()) {
			throw new OperationException("error.gateway_without_connection", description.getGatewayId());
		}
		log.info("[{}] 模拟网关收到撤单请求：{}", description.getGatewayId(), originOrderId);
		account.cancelOrder(originOrderId);
		account.getPositionManager().positionFields().forEach(pf -> feEngine.emitEvent(NorthstarEventType.POSITION, pf));
		return true;
	}

	private boolean isConnected() {
		return connection == ConnectionState.CONNECTED;
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

	public void onTick(Tick tick) {
		if(tick.contract().tradable()) {			
			mktCenter.onTick(tick);
		}
		account.onTick(tick);
	}

}
