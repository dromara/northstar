package tech.xuanwu.northstar.gateway.sim;

import java.util.Optional;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.exception.TradeException;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.gateway.sim.persistence.SimAccountPO;
import tech.xuanwu.northstar.gateway.sim.persistence.SimAccountRepository;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public class SimGatewayLocalImpl implements SimGateway{
	
	private FastEventEngine feEngine;
	
	private GatewaySettingField gatewaySetting;
	
	private boolean connected;
	
	private GwAccountHolder accountHolder;
	
	private SimAccountRepository simAccRepo;
	
	public SimGatewayLocalImpl(FastEventEngine feEngine, GatewaySettingField gatewaySetting,
			SimAccountRepository simAccRepo, SimFactory factory) {
		this.feEngine = feEngine;
		this.gatewaySetting = gatewaySetting;
		this.accountHolder = factory.newGwAccountHolder();
	}

	@Override
	public GatewaySettingField getGatewaySetting() {
		return gatewaySetting;
	}

	@Override
	public void connect() {
		connected = true;
		feEngine.emitEvent(NorthstarEventType.LOGGED_IN, gatewaySetting.getGatewayId());
	}

	@Override
	public void disconnect() {
		connected = false;
		feEngine.emitEvent(NorthstarEventType.LOGGED_OUT, gatewaySetting.getGatewayId());
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
		return accountHolder.submitOrder(submitOrderReq);
	}

	@Override
	public boolean cancelOrder(CancelOrderReqField cancelOrderReq) {
		return accountHolder.cancelOrder(cancelOrderReq);
	}

	@Override
	public void moneyIO(int money) {
		if(money > 0) {			
			accountHolder.deposit(money);
		} else if(money < 0) {
			accountHolder.withdraw(money);
		}
	}

	@Override
	public void update(TickField tick) {
		accountHolder.updateTick(tick);
	}

	@Override
	public void save() {
		SimAccountPO po = accountHolder.convertTo();
		simAccRepo.save(po);
	}

	@Override
	public boolean load() {
		Optional<SimAccountPO> po = simAccRepo.findById(gatewaySetting.getGatewayId());
		if(po.isEmpty()) {
			return false;
		}
		
		boolean flag = true;
		try {
			accountHolder.convertFrom(po.get());
		} catch (InvalidProtocolBufferException e) {
			log.warn("无法加载历史记录", e);
			flag = false;
		}
		return flag;
	}

	@Override
	public void remove() {
		simAccRepo.deleteById(gatewaySetting.getGatewayId());
	}
}
