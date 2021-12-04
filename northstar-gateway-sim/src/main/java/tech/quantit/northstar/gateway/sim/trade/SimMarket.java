package tech.quantit.northstar.gateway.sim.trade;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import tech.quantit.northstar.gateway.sim.persistence.SimAccountPO;
import tech.quantit.northstar.gateway.sim.persistence.SimAccountRepository;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;
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
	
	private SimAccountRepository simAccRepo;
	
	public SimMarket(SimAccountRepository simAccRepo) {
		this.simAccRepo = simAccRepo;
	}
	
	public synchronized void addGateway(String mdGatewayId, SimTradeGateway accountGateway) {
		String simGatewayId = accountGateway.getGatewaySetting().getGatewayId();
		simGatewayMap.put(mdGatewayId, simGatewayId, accountGateway);
	}
	
	public synchronized void removeGateway(String mdGatewayId, SimTradeGateway accountGateway) {
		String simGatewayId = accountGateway.getGatewaySetting().getGatewayId();
		SimTradeGateway simTradeGateway = simGatewayMap.remove(mdGatewayId, simGatewayId);
		remove(simTradeGateway.getGatewaySetting().getGatewayId());
	}
	
	public void onTick(TickField tick) {
		Map<String, SimTradeGateway> simGateways = simGatewayMap.row(tick.getGatewayId());
		simGateways.forEach((k, gw) -> {
			gw.onTick(tick);
		});
	}
	
	public void onTrade(TradeField trade) {
		Map<String, SimTradeGateway> simGateways = simGatewayMap.column(trade.getGatewayId());
		simGateways.forEach((k, gw) -> {
			save(gw.getAccount());
		});
	}
	
	/**
	 * 保存模拟账户
	 */
	public void save(GwAccountHolder accountHolder) {
		SimAccountPO po = accountHolder.convertTo();
		simAccRepo.save(po);
	}
	
	/**
	 * 载入模拟账户
	 */
	public Optional<SimAccountPO> load(String gatewayId) {
		 return simAccRepo.findById(gatewayId);
	}
	
	/**
	 * 移除模拟账户
	 */
	public void remove(String gatewayId) {
		simAccRepo.deleteById(gatewayId);
	}
}
