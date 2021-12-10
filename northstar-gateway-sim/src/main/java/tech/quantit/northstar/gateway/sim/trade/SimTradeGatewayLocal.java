package tech.quantit.northstar.gateway.sim.trade;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
	
	private ScheduledExecutorService execService = Executors.newScheduledThreadPool(1);
	
	private ScheduledFuture<?> job;
	
	private SimContractGenerator contractGen = new SimContractGenerator();
	
	public SimTradeGatewayLocal(FastEventEngine feEngine, GatewaySettingField gatewaySetting, SimAccount account) {
		this.feEngine = feEngine;
		this.gatewaySetting = gatewaySetting;
		this.account = account;	
	}

	@Override
	public void connect() {
		connected = true;
		feEngine.emitEvent(NorthstarEventType.CONNECTED, gatewaySetting.getGatewayId());
		feEngine.emitEvent(NorthstarEventType.LOGGED_IN, gatewaySetting.getGatewayId());
		
		job = execService.scheduleAtFixedRate(()->{
			log.trace("模拟账户定时回报");
			AccountField af = account.accountField();
			feEngine.emitEvent(NorthstarEventType.ACCOUNT, af);
			log.trace("账户信息：{}", af);
			boolean shouldClear = false;
			for(PositionField pf : account.positionFields()) {
				feEngine.emitEvent(NorthstarEventType.POSITION, pf);
				log.trace("持仓信息：{}", pf);
				if(pf.getPosition() == 0) {
					shouldClear = true;
				}
			}
			if(shouldClear) {
				account.removeEmptyPosition();
			}
			
		}, 2, 2, TimeUnit.SECONDS);
		
		// 模拟返回合约
		CompletableFuture.runAsync(()->{
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				log.error("", e);
			}
			
			feEngine.emitEvent(NorthstarEventType.CONTRACT, contractGen.getContract(gatewaySetting.getGatewayId()));
			feEngine.emitEvent(NorthstarEventType.CONTRACT_LOADED, gatewaySetting.getGatewayId());
		});
	}

	@Override
	public void disconnect() {
		connected = false;
		feEngine.emitEvent(NorthstarEventType.DISCONNECTED, gatewaySetting.getGatewayId());
		feEngine.emitEvent(NorthstarEventType.LOGGED_OUT, gatewaySetting.getGatewayId());
		
		if(job.cancel(true)) {
			log.info("模拟账户定时回报任务结束");
		} else {
			log.info("模拟账户定时回报任务已经结束");
		}
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

}
