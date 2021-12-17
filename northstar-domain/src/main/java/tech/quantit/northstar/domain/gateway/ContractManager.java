package tech.quantit.northstar.domain.gateway;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import xyz.redtorch.pb.CoreField.ContractField;

@Slf4j
public class ContractManager {
	
	private static final int DEFAULT_SIZE = 4096;
	
	/**
	 * gateway -> symbol -> contract
	 */
	private Table<String, String, ContractField> contractTbl = HashBasedTable.create();
	/**
	 * unifiedSymbol -> contract
	 */
	private Map<String, ContractField> contractMap = new HashMap<>(DEFAULT_SIZE);
	
	
	public ContractManager(GlobalMarketRegistry registry) {
		registry.setOnContractSubsciptionCallback(this::addContract);
	}
	
	public synchronized boolean addContract(ContractField contract) {
		String gatewayId = contract.getGatewayId();
		String symbol = contract.getSymbol();
		String unifiedSymbol = contract.getUnifiedSymbol();
		contractMap.put(unifiedSymbol, contract);
		contractTbl.put(gatewayId, symbol, contract);
		log.info("加入合约：网关{}, 合约{}, 网关累计总合约数{}个", gatewayId, symbol, contractTbl.row(gatewayId).size());
		return true;
	}
	
	public synchronized ContractField getContract(String gatewayId, String symbol) {
		ContractField result = contractTbl.get(gatewayId, symbol);
		if(result == null) {
			throw new NoSuchElementException("找不到合约：" + gatewayId + "_" + symbol);
		}
		return result;
	}
	
	public synchronized ContractField getContract(String unifiedSymbol) {
		if(!contractMap.containsKey(unifiedSymbol)) {
			throw new NoSuchElementException("找不到合约：" + unifiedSymbol);
		}
		return contractMap.get(unifiedSymbol);
	}
	
	public synchronized Collection<ContractField> getAllContracts(){
		return contractMap.values();
	}
	
	public synchronized Map<String, ContractField> getContractMapByGateway(String gatewayId){
		return contractTbl.row(gatewayId);
	}
	
	public synchronized void clear(String gatewayId) {
		Map<String, ContractField> gatewayContractMap = getContractMapByGateway(gatewayId);
		for(Entry<String, ContractField> e : gatewayContractMap.entrySet()) {
			contractTbl.remove(gatewayId, e.getKey());
			contractMap.remove(e.getValue().getUnifiedSymbol());
		}
	}
	
}
