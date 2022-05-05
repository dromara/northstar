package tech.quantit.northstar.common.utils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;

public class ContractUtils {

	private ContractUtils() {}
	
	/**
	 * 返回未来13个月的合约符号，包含当月
	 * @param unifiedSymbolOfIndexContract
	 * @param exchange
	 * @return
	 */
	public static List<String> getMonthlyUnifiedSymbolOfIndexContract(String unifiedSymbolOfIndexContract, ExchangeEnum exchange){
		List<String> resultList = new ArrayList<>();
		for(int i=0; i<13; i++) {
			LocalDate now = LocalDate.now();
			
		}
		return resultList;
	}
}
