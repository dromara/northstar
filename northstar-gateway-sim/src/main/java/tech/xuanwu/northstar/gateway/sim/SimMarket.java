package tech.xuanwu.northstar.gateway.sim;

import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import tech.xuanwu.northstar.gateway.api.Gateway;
import xyz.redtorch.pb.CoreField.TickField;

public class SimMarket {

	/**
	 * dataSrcGatewayId -> simGatewayId -> simGateway
	 */
	private Table<String, String, SimGatewayLocalImpl> simGatewayMap = HashBasedTable.create();
	
	
	public synchronized void addGateway(Gateway dataSrcGateway, SimGatewayLocalImpl accountGateway) {
		String dataSrcGatewayId = dataSrcGateway.getGatewaySetting().getGatewayId();
		String simGatewayId = accountGateway.getGatewaySetting().getGatewayId();
		simGatewayMap.put(dataSrcGatewayId, simGatewayId, accountGateway);
	}
	
	public synchronized void removeGateway(Gateway dataSrcGateway, SimGatewayLocalImpl accountGateway) {
		String dataSrcGatewayId = dataSrcGateway.getGatewaySetting().getGatewayId();
		String simGatewayId = accountGateway.getGatewaySetting().getGatewayId();
		simGatewayMap.remove(dataSrcGatewayId, simGatewayId);
	}
	
	public void update(TickField tick) {
		Map<String, SimGatewayLocalImpl> simGateways = simGatewayMap.row(tick.getGatewayId());
		simGateways.forEach((k, gw) -> {
			gw.update(tick);
		});
	}
}
