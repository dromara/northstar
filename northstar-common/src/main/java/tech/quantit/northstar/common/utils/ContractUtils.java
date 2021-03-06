package tech.quantit.northstar.common.utils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import tech.quantit.northstar.common.constant.Constants;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreField.ContractField;

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
		LocalDate now = LocalDate.now();
		for(int i=0; i<13; i++) {
			LocalDate date = now.plusMonths(i);
			int year = date.getYear();
			int month = date.getMonthValue();
			String yearStr = exchange == ExchangeEnum.CZCE ? String.valueOf(year).substring(3) : String.valueOf(year).substring(2);
			String monthStr = String.format("%02d", month);
			String symbol = unifiedSymbolOfIndexContract.replace(Constants.INDEX_SUFFIX, yearStr + monthStr);
			resultList.add(symbol);
		}
		return resultList;
	}
	
	public static String getMarketGatewayId(ContractField contract) {
		return contract.getThirdPartyId().split("@")[1];
	}
}
