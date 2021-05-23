package tech.xuanwu.northstar.gateway.sim;

import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import xyz.redtorch.pb.CoreField.TickField;

public class SimMarket {

	/**
	 * mdGatewayId -> simGatewayId -> simGateway
	 */
	private Table<String, String, SimGateway> simGatewayMap = HashBasedTable.create();
	
	
	public synchronized void addGateway(String mdGatewayId, SimGateway accountGateway) {
		String simGatewayId = accountGateway.getGatewaySetting().getGatewayId();
		simGatewayMap.put(mdGatewayId, simGatewayId, accountGateway);
	}
	
	public synchronized void removeGateway(String mdGatewayId, SimGateway accountGateway) {
		String simGatewayId = accountGateway.getGatewaySetting().getGatewayId();
		simGatewayMap.remove(mdGatewayId, simGatewayId);
	}
	
	public void update(TickField tick) {
		Map<String, SimGateway> simGateways = simGatewayMap.row(tick.getGatewayId());
		simGateways.forEach((k, gw) -> {
			gw.update(tick);
		});
	}
}
