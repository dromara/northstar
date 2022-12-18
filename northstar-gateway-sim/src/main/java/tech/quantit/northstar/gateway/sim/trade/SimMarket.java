package tech.quantit.northstar.gateway.sim.trade;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
	
	private Executor exec = Executors.newSingleThreadExecutor();	// 增加一个工作线程解耦TICK事件可能导致的死锁问题
	
	public synchronized void addGateway(String mdGatewayId, SimTradeGateway accountGateway) {
		String simGatewayId = accountGateway.getGatewaySetting().getGatewayId();
		simGatewayMap.put(mdGatewayId, simGatewayId, accountGateway);
	}
	
	public synchronized void removeGateway(String mdGatewayId, SimTradeGateway accountGateway) {
		String simGatewayId = accountGateway.getGatewaySetting().getGatewayId();
		simGatewayMap.remove(mdGatewayId, simGatewayId);
	}
	
	public void onTick(TickField tick) {
		exec.execute(() ->
			simGatewayMap.values().stream()
				.map(SimTradeGatewayLocal.class::cast)
				.map(SimTradeGatewayLocal::getAccount)
				.forEach(simAccount -> simAccount.onTick(tick))
		);
	}
	
}
