package tech.xuanwu.northstar.engine.index;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.utils.ContractNameResolver;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 指数引擎
 * @author KevinHuangwl
 *
 */
public class IndexEngine {
	
	/**
	 * gateway -> symbolGroup -> idxContract
	 */
	private Table<String, String, IndexContract> idxContractTbl = HashBasedTable.create();
	
	/**
	 * gateway -> symbolGroup -> contractSet
	 */
	private Table<String, String, Set<ContractField>> contractGroupTbl = HashBasedTable.create();
	
	private volatile boolean isRunning;
	
	private FastEventEngine feEngine;
	
	public IndexEngine(FastEventEngine fastEventEngine) {
		this.feEngine = fastEventEngine;
	}

	public void updateTick(TickField tick) {
		if(!isRunning) {
			return;
		}
		String gatewayId = tick.getGatewayId();
		String unifiedSymbol = tick.getUnifiedSymbol();
		String symbolGroup = ContractNameResolver.unifiedSymbolToSymbolGroup(unifiedSymbol);
		if(idxContractTbl.contains(gatewayId, symbolGroup)) {
			idxContractTbl.get(gatewayId, symbolGroup).updateByTick(tick);
		}
	}
	
	public void registerContract(ContractField contract) {
		String gatewayId = contract.getGatewayId();
		String symbolGroup = ContractNameResolver.unifiedSymbolToSymbolGroup(contract.getUnifiedSymbol());
		if(!contractGroupTbl.contains(gatewayId, symbolGroup)) {
			contractGroupTbl.put(gatewayId, symbolGroup, new HashSet<>());
		}
		contractGroupTbl.get(gatewayId, symbolGroup).add(contract);
	}
	
	public void start() {
		for(Cell<String, String, Set<ContractField>> cell : contractGroupTbl.cellSet()) {
			IndexContract idxContract = new IndexContract(cell.getValue(), (tick) -> feEngine.emitEvent(NorthstarEventType.IDX_TICK, tick));
			idxContractTbl.put(cell.getRowKey(), cell.getColumnKey(), idxContract);
			feEngine.emitEvent(NorthstarEventType.IDX_CONTRACT, idxContract.getContract());
		}
		isRunning = true;
	}
	
}
