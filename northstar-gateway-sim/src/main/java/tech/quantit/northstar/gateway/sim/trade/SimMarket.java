package tech.quantit.northstar.gateway.sim.trade;

import java.util.function.Consumer;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.eventbus.EventBus;

import lombok.Getter;
import xyz.redtorch.pb.CoreField.TickField;
/**
 * 用于管理Gateway实例及模拟账户持久化操作
 * @author KevinHuangwl
 *
 */
public class SimMarket {

	/**
	 * mdGatewayId -> simGatewayId -> simGateway
	 */
	private Table<String, String, SimTradeGateway> simGatewayMap = HashBasedTable.create();
	
	@Getter
	private EventBus marketEventBus = new EventBus();
	
	private Consumer<SimTradeGateway> removeCallback;
	
	public SimMarket(Consumer<SimTradeGateway> removeCallback) {
		this.removeCallback = removeCallback;
	}
	
	public synchronized void addGateway(String mdGatewayId, SimTradeGateway accountGateway) {
		String simGatewayId = accountGateway.getGatewaySetting().getGatewayId();
		simGatewayMap.put(mdGatewayId, simGatewayId, accountGateway);
		SimTradeGatewayLocal gateway = (SimTradeGatewayLocal) accountGateway;
		SimAccount simAccount = gateway.getAccount();
		simAccount.setEventBus(marketEventBus);
		marketEventBus.register(simAccount);
	}
	
	public synchronized void removeGateway(String mdGatewayId, SimTradeGateway accountGateway) {
		String simGatewayId = accountGateway.getGatewaySetting().getGatewayId();
		simGatewayMap.remove(mdGatewayId, simGatewayId);
		SimTradeGatewayLocal simGateway = (SimTradeGatewayLocal) accountGateway;
		SimAccount simAccount = simGateway.getAccount();
		removeCallback.accept(accountGateway);
		marketEventBus.unregister(simAccount);
	}
	
	public void onTick(TickField tick) {
		marketEventBus.post(tick);
	}
	
}
