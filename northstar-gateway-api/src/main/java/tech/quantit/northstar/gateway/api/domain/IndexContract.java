package tech.quantit.northstar.gateway.api.domain;

import java.util.Set;
import java.util.stream.Collectors;

import xyz.redtorch.pb.CoreField.ContractField;

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
		super.field = ContractField.newBuilder(protoContract)
				.setSymbol(symbol)
				.setUnifiedSymbol(idxSymbol)
				.setFullName(fullName)
				.setName(name)
				.build();
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
