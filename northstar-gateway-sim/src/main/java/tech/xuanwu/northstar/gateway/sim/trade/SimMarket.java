package tech.xuanwu.northstar.gateway.sim.trade;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import tech.xuanwu.northstar.gateway.sim.persistence.SimAccountPO;
import tech.xuanwu.northstar.gateway.sim.persistence.SimAccountRepository;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class SimMarket {

	/**
	 * mdGatewayId -> simGatewayId -> simGateway
	 */
	private Table<String, String, SimGateway> simGatewayMap = HashBasedTable.create();
	
	private SimAccountRepository simAccRepo;
	
	public SimMarket(SimAccountRepository simAccRepo) {
		this.simAccRepo = simAccRepo;
	}
	
	public synchronized void addGateway(String mdGatewayId, SimGateway accountGateway) {
		String simGatewayId = accountGateway.getGatewaySetting().getGatewayId();
		simGatewayMap.put(mdGatewayId, simGatewayId, accountGateway);
	}
	
	public synchronized void removeGateway(String mdGatewayId, SimGateway accountGateway) {
		String simGatewayId = accountGateway.getGatewaySetting().getGatewayId();
		SimGateway simGateway = simGatewayMap.remove(mdGatewayId, simGatewayId);
		remove(simGateway.getGatewaySetting().getGatewayId());
	}
	
	public void onTick(TickField tick) {
		Map<String, SimGateway> simGateways = simGatewayMap.row(tick.getGatewayId());
		simGateways.forEach((k, gw) -> {
			gw.onTick(tick);
		});
	}
	
	public void onTrade(TradeField trade) {
		Map<String, SimGateway> simGateways = simGatewayMap.column(trade.getGatewayId());
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
