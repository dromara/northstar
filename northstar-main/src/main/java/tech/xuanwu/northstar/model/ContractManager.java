package tech.xuanwu.northstar.model;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

@Slf4j
public class ContractManager {
	
	private static final int DEFAULT_SIZE = 15000;
	private Table<String, String, ContractField> contractTbl = HashBasedTable.create();
	private Map<String, WeakReference<ContractField>> contractMap = new HashMap<>(DEFAULT_SIZE);
	private List<WeakReference<ContractField>> contractList = new LinkedList<>();
	
	private Set<ProductClassEnum> canHandleTypes = new HashSet<>();
	public ContractManager(String... contractTypes) {
		for(String type : contractTypes) {
			canHandleTypes.add(ProductClassEnum.valueOf(ProductClassEnum.class, type));
		}
	}
	
	public void addContract(ContractField contract) {
		String gatewayId = contract.getGatewayId();
		String symbol = contract.getSymbol();
		String unifiedSymbol = contract.getUnifiedSymbol();
		if(!canHandleTypes.contains(contract.getProductClass())) {
			return;
		}
		contractMap.put(unifiedSymbol, new WeakReference<>(contract));
		contractTbl.put(gatewayId, symbol, contract);
		contractList.add(new WeakReference<>(contract));
		log.info("加入合约：网关{}, 合约{}, 累计总合约数{}个", gatewayId, symbol, contractList.size());
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
	
	public Collection<ContractField> getAllContracts(){
		return contractList.stream()
				.filter(i -> i.get() != null)
				.map(i -> i.get())
				.collect(Collectors.toList());
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
