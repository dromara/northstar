package tech.quantit.northstar.gateway.sim.trade;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.exception.TradeException;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public class SimTradeGatewayLocal implements SimTradeGateway{
	
	private FastEventEngine feEngine;
	
	private GatewaySettingField gatewaySetting;
	
	private boolean connected;
	
	private GwAccountHolder accountHolder;
	
	private ScheduledExecutorService execService = Executors.newScheduledThreadPool(1);
	
	private ScheduledFuture<?> job;
	
	public SimTradeGatewayLocal(FastEventEngine feEngine, GatewaySettingField gatewaySetting, GwAccountHolder accountHolder) {
		this.feEngine = feEngine;
		this.gatewaySetting = gatewaySetting;
		this.accountHolder = accountHolder;	
	}

	@Override
	public GatewaySettingField getGatewaySetting() {
		return gatewaySetting;
	}

	@Override
	public void connect() {
		connected = true;
		feEngine.emitEvent(NorthstarEventType.CONNECTED, gatewaySetting.getGatewayId());
		feEngine.emitEvent(NorthstarEventType.LOGGED_IN, gatewaySetting.getGatewayId());
		
		job = execService.scheduleAtFixedRate(()->{
			accountHolder.emitStatus();
		}, 2, 2, TimeUnit.SECONDS);
		
		// 模拟返回合约
		CompletableFuture.runAsync(()->{
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				log.error("", e);
			}
			LocalDate date = LocalDate.now().plusDays(45);
			String year = date.getYear() % 100 + "";
			String month = String.format("%02d", date.getMonth().getValue());
			String symbol = "sim" + year + month;
			String name = "模拟品种" + year + month;
			feEngine.emitEvent(NorthstarEventType.CONTRACT, ContractField.newBuilder()
					.setGatewayId(gatewaySetting.getGatewayId())
					.setContractId(symbol + "@SHFE@FUTURES@" + gatewaySetting.getGatewayId())
					.setCurrency(CurrencyEnum.CNY)
					.setExchange(ExchangeEnum.SHFE)
					.setFullName(name)
					.setName(name)
					.setUnifiedSymbol(symbol + "@SHFE@FUTURES")
					.setSymbol(symbol)
					.setProductClass(ProductClassEnum.FUTURES)
					.setThirdPartyId(symbol)
					.setMultiplier(1)
					.setPriceTick(10)
					.setLongMarginRatio(0.08)
					.setShortMarginRatio(0.08)
					.build());
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
	public boolean isConnected() {
		return connected;
	}

	@Override
	public boolean getAuthErrorFlag() {
		return false;
	}

	@Override
	public String submitOrder(SubmitOrderReqField submitOrderReq) throws TradeException {
		SubmitOrderReqField orderReq = SubmitOrderReqField.newBuilder(submitOrderReq).setGatewayId(gatewaySetting.getGatewayId()).build();
		return accountHolder.submitOrder(orderReq);
	}

	@Override
	public boolean cancelOrder(CancelOrderReqField cancelOrderReq) {
		return accountHolder.cancelOrder(cancelOrderReq);
	}

	@Override
	public int moneyIO(int money) {
		if(money >= 0) {			
			return accountHolder.deposit(money);
		}
		return accountHolder.withdraw(Math.abs(money));
	}

	@Override
	public void onTick(TickField tick) {
		if(!connected) return;
		accountHolder.updateTick(tick);
	}

	@Override
	public GwAccountHolder getAccount() {
		return accountHolder;
	}
	
}
