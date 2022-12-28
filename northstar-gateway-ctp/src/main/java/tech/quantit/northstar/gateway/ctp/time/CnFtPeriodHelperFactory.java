package tech.quantit.northstar.gateway.ctp.time;

import org.springframework.util.Assert;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import tech.quantit.northstar.gateway.api.domain.contract.ContractDefinition;
import tech.quantit.northstar.gateway.api.domain.time.GenericTradeTime;
import tech.quantit.northstar.gateway.api.domain.time.IPeriodHelperFactory;
import tech.quantit.northstar.gateway.api.domain.time.PeriodHelper;

public class CnFtPeriodHelperFactory implements IPeriodHelperFactory{
	
	private static final PeriodHelper GENERIC_HELPER = new PeriodHelper(1, new GenericTradeTime());
	
	private Table<String, Integer, PeriodHelper> helperCache = HashBasedTable.create();
	private Table<String, Integer, PeriodHelper> helperCache2 = HashBasedTable.create();

	public PeriodHelper newInstance(int numbersOfMinPerPeriod, boolean exclusiveOpenning, ContractDefinition contractDef) {
		Assert.isTrue(numbersOfMinPerPeriod > 0, "分钟周期数应该大于0");
		
		if(getHelperCache(exclusiveOpenning).contains(contractDef.getTradeTimeType(), numbersOfMinPerPeriod)) {
			return getHelperCache(exclusiveOpenning).get(contractDef.getTradeTimeType(), numbersOfMinPerPeriod);
		}
		
		PeriodHelper helper = switch(contractDef.getTradeTimeType()) {
		case "CN_FT_TT1" -> new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime1(), exclusiveOpenning);
		case "CN_FT_TT2" -> new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime2(), exclusiveOpenning);
		case "CN_FT_TT3" -> new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime3(), exclusiveOpenning);
		case "CN_FT_TT4" -> new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime4(), exclusiveOpenning);
		case "CN_FT_TT5" -> new PeriodHelper(numbersOfMinPerPeriod, new CnFtIndexTradeTime(), exclusiveOpenning);
		case "CN_FT_TT6" -> new PeriodHelper(numbersOfMinPerPeriod, new CnFtBondTradeTime(), exclusiveOpenning);
		default -> GENERIC_HELPER;
		};
		getHelperCache(exclusiveOpenning).put(contractDef.getTradeTimeType(), numbersOfMinPerPeriod, helper);
		return helper;
	}
	
	private Table<String, Integer, PeriodHelper> getHelperCache(boolean exclusiveOpenning) {
		return exclusiveOpenning ? helperCache : helperCache2;
	}
	
}
