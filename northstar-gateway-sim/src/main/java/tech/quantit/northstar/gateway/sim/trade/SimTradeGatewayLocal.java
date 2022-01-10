package tech.quantit.northstar.gateway.sim.trade;

import java.util.concurrent.CompletableFuture;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.exception.TradeException;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import tech.quantit.northstar.gateway.api.domain.NormalContract;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
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
	
	private SimContractGenerator contractGen;
	
	private GlobalMarketRegistry registry;
	
	public SimTradeGatewayLocal(FastEventEngine feEngine, GatewaySettingField gatewaySetting, SimAccount account, GlobalMarketRegistry registry) {
		this.feEngine = feEngine;
		this.gatewaySetting = gatewaySetting;
		this.account = account;	
		this.registry = registry;
		this.contractGen = new SimContractGenerator(gatewaySetting.getGatewayId());
	}

	@Override
	public void connect() {
		log.debug("[{}] 模拟网关连线", gatewaySetting.getGatewayId());
		connected = true;
		feEngine.emitEvent(NorthstarEventType.CONNECTED, gatewaySetting.getGatewayId());
		feEngine.emitEvent(NorthstarEventType.LOGGED_IN, gatewaySetting.getGatewayId());
		
		// 阻塞一下，防止账户回报比连线回报要快导致异常
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			log.error("", e);
		}
		
		AccountField af = account.accountField();
		feEngine.emitEvent(NorthstarEventType.ACCOUNT, af);
		
		for(PositionField pf : account.positionFields()) {
			feEngine.emitEvent(NorthstarEventType.POSITION, pf);
		}
		
		// 模拟返回合约
		CompletableFuture.runAsync(()->{
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				log.error("", e);
			}
			
			ContractField simContract = contractGen.getContract();
			ContractField simContract2 = contractGen.getContract2();
			registry.register(new NormalContract(simContract, GatewayType.SIM, System.currentTimeMillis()));
			registry.register(new NormalContract(simContract2, GatewayType.SIM, System.currentTimeMillis()));
		});
	}

	@Override
	public void disconnect() {
		log.debug("[{}] 模拟网关断开", gatewaySetting.getGatewayId());
		connected = false;
		feEngine.emitEvent(NorthstarEventType.DISCONNECTED, gatewaySetting.getGatewayId());
		feEngine.emitEvent(NorthstarEventType.LOGGED_OUT, gatewaySetting.getGatewayId());
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
