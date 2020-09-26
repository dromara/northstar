package tech.xuanwu.northstar.trader.domain.contract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tech.xuanwu.northstar.constant.CommonConstant;
import tech.xuanwu.northstar.gateway.FastEventEngine;
import tech.xuanwu.northstar.gateway.FastEventEngine.EventType;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 
 * @author kevinhuangwl
 *
 */
@Component
public class IndexContractMaker {
	
	private Map<String, List<ContractField>> contractGroupMap = new HashMap<>(200);
	
	private Map<String, IndexContract> contractToIndexContractMap = new HashMap<>(200);
	
	@Autowired
	private FastEventEngine feEngine;
	
	private Set<String> nameSet = new HashSet<>(200);
	
	public void initFrom(Map<String, ContractField> contractMap) {
		contractMap.forEach((k, contract)->{
			String symbolName = resolveSymbolName(contract.getUnifiedSymbol());
			if(!nameSet.contains(symbolName)) {
				nameSet.add(symbolName);
				contractGroupMap.put(symbolName, new ArrayList<>());
			}
			contractGroupMap.get(symbolName).add(contract);
		});
		
		contractGroupMap.forEach((k, list) 
				-> contractToIndexContractMap.put(k, new IndexContract(list, (tick) 
						-> feEngine.emitEvent(EventType.TICK, "", tick))));
	}

	public void updateTick(TickField tick) {
		// 过滤指数合约的TICK
		if(tick.getUnifiedSymbol().contains(CommonConstant.INDEX_SUFFIX)) {				
			return;
		}
		String symbolName = resolveSymbolName(tick.getUnifiedSymbol());
		IndexContract idxContract = contractToIndexContractMap.get(symbolName);
		if(idxContract == null) {
			return;
		}
		idxContract.updateByTick(tick);			
	}

	private String resolveSymbolName(String unifiedSymbol) {
		String[] parts = unifiedSymbol.split("@");
		String symbol = parts[0];
		return symbol.replaceAll("\\d+", "");
	}
}
