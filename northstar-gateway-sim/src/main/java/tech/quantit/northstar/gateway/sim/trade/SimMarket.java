package tech.quantit.northstar.gateway.sim.trade;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

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
	
	public synchronized void addGateway(String mdGatewayId, SimTradeGateway accountGateway) {
		String simGatewayId = accountGateway.getGatewaySetting().getGatewayId();
		simGatewayMap.put(mdGatewayId, simGatewayId, accountGateway);
	}
	
	public synchronized void removeGateway(String mdGatewayId, SimTradeGateway accountGateway) {
		String simGatewayId = accountGateway.getGatewaySetting().getGatewayId();
		simGatewayMap.remove(mdGatewayId, simGatewayId);
	}
	
	public void onTick(TickField tick) {
		simGatewayMap.values().stream()
			.map(SimTradeGatewayLocal.class::cast)
			.map(SimTradeGatewayLocal::getAccount)
			.forEach(simAccount -> simAccount.onTick(tick));
	}
	
}
