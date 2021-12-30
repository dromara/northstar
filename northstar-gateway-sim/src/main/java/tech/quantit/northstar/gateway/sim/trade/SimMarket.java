package tech.quantit.northstar.gateway.sim.trade;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.eventbus.EventBus;

import lombok.Getter;
import tech.quantit.northstar.gateway.sim.persistence.SimAccountRepository;
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
	private EventBus marketEventBus = new EventBus("SimMarketEventBus");
	
	private SimAccountRepository simAccRepo;
	
	public SimMarket(SimAccountRepository simAccRepo) {
		this.simAccRepo = simAccRepo;
	}
	
	public synchronized void addGateway(String mdGatewayId, SimTradeGateway accountGateway) {
		String simGatewayId = accountGateway.getGatewaySetting().getGatewayId();
		simGatewayMap.put(mdGatewayId, simGatewayId, accountGateway);
		SimTradeGatewayLocal gateway = (SimTradeGatewayLocal) accountGateway;
		gateway.getAccount().setEventBus(marketEventBus);
	}
	
	public synchronized void removeGateway(String mdGatewayId, SimTradeGateway accountGateway) {
		String simGatewayId = accountGateway.getGatewaySetting().getGatewayId();
		simGatewayMap.remove(mdGatewayId, simGatewayId);
		simAccRepo.deleteById(simGatewayId);
	}
	
	public void onTick(TickField tick) {
		marketEventBus.post(tick);
	}
	
}
