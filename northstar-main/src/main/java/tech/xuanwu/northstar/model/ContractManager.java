package tech.xuanwu.northstar.model;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import xyz.redtorch.pb.CoreField.ContractField;

public class ContractManager {
	
	private Table<String, String, ContractField> contractTbl = HashBasedTable.create();
	private Map<String, WeakReference<ContractField>> contractMap = new HashMap<>();

	public void addContract(ContractField contract) {
		String gatewayId = contract.getGatewayId();
		String symbol = contract.getSymbol();
		String unifiedSymbol = contract.getUnifiedSymbol();
		contractMap.put(unifiedSymbol, new WeakReference<>(contract));
		contractTbl.put(gatewayId, symbol, contract);
	}
	
	public ContractField getContract(String gatewayId, String symbol) {
		ContractField result = contractTbl.get(gatewayId, symbol);
		if(result == null) {
			throw new NoSuchElementException("找不到合约：" + gatewayId + "_" + symbol);
		}
		return result;
	}
	
	public ContractField getContract(String unifiedSymbol) {
		ContractField result = contractMap.get(unifiedSymbol).get();
		if(result == null) {
			throw new NoSuchElementException("找不到合约：" + unifiedSymbol);
		}
		return result;
	}
	
	public Map<String, ContractField> getContractMapByGateway(String gatewayId){
		return contractTbl.row(gatewayId);
	}
	
	public void clear(String gatewayId) {
		Map<String, ContractField> gatewayContractMap = getContractMapByGateway(gatewayId);
		for(Entry<String, ContractField> e : gatewayContractMap.entrySet()) {
			contractTbl.remove(gatewayId, e.getKey());
		}
	}
}
