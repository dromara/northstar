package tech.quantit.northstar.gateway.api.domain;

import java.util.Set;
import java.util.stream.Collectors;

import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 指数合约
 * @author KevinHuangwl
 *
 */
@Deprecated
public class IndexContract extends NormalContract {
	
	private IndexTicker ticker;
	
	private Set<ContractField> monthlyContracts;

	public IndexContract(String idxSymbol, Set<ContractField> monthlyContracts) {
		if(monthlyContracts.isEmpty()) {
			throw new IllegalArgumentException("不能传入空集合");
		}
		ContractField protoContract = monthlyContracts.iterator().next();
		String name = protoContract.getName().replaceAll("\\d+", "指数");
		String fullName = protoContract.getFullName().replaceAll("\\d+", "指数");
		String symbol = idxSymbol.replaceAll("([A-z]+\\d{3,4})@\\w+@\\w+", "$1");
		String originSymbol = protoContract.getSymbol();
		String contractId = protoContract.getContractId().replace(originSymbol, symbol);
		String thirdPartyId = protoContract.getThirdPartyId().replace(originSymbol, symbol);
		super.field = ContractField.newBuilder(protoContract)
				.setSymbol(symbol)
				.setThirdPartyId(thirdPartyId)
				.setContractId(contractId)
				.setLastTradeDateOrContractMonth("")
				.setUnifiedSymbol(idxSymbol)
				.setFullName(fullName)
				.setLongMarginRatio(0.1)
				.setShortMarginRatio(0.1)
				.setName(name)
				.build();
		super.gatewayType = protoContract.getThirdPartyId().split("@")[1];
		super.updateTime = System.currentTimeMillis();
		this.monthlyContracts = monthlyContracts;
	}

	public IndexTicker indexTicker() {
		if(ticker == null) {			
			ticker = new IndexTicker(this);
		}
		return ticker;
	}
	
	public Set<String> monthlyContractSymbols(){
		return monthlyContracts.stream().map(ContractField::getUnifiedSymbol).collect(Collectors.toSet());
	}
	
}
