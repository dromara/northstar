package tech.quantit.northstar.gateway.api.domain.time;

import java.util.List;
import java.util.Objects;

import org.springframework.util.Assert;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import tech.quantit.northstar.common.model.ContractDefinition;
import xyz.redtorch.pb.CoreField.ContractField;

public class PeriodHelperFactory {
	
	private static final PeriodHelper GENERIC_HELPER = new PeriodHelper(1, new GenericTradeTime());
	private List<ContractDefinition> contractDefs;
	
	private Table<String, Integer, PeriodHelper> helperCache = HashBasedTable.create();
	private Table<String, Integer, PeriodHelper> helperCache2 = HashBasedTable.create();
	
	public PeriodHelperFactory(List<ContractDefinition> contractDefs) {
		this.contractDefs = contractDefs;
	}

	public PeriodHelper newInstance(int numbersOfMinPerPeriod, boolean segregateOpenning, ContractField contract) {
		Assert.isTrue(numbersOfMinPerPeriod > 0, "分钟周期数应该大于0");
		ContractDefinition cd = findDefinition(contract);
		if(Objects.isNull(cd)) {
			return GENERIC_HELPER;
		}
		
		if(getHelperCache(segregateOpenning).contains(cd.getTradeTimeType(), numbersOfMinPerPeriod)) {
			return getHelperCache(segregateOpenning).get(cd.getTradeTimeType(), numbersOfMinPerPeriod);
		}
		
		PeriodHelper helper = switch(cd.getTradeTimeType()) {
		case "CN_FT_TT1" -> segregateOpenning ? new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime1()) : new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime1(), TradeTimeConstant.CN_FT_NIGHT_OPENNING);
		case "CN_FT_TT2" -> segregateOpenning ? new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime2()) : new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime2(), TradeTimeConstant.CN_FT_NIGHT_OPENNING);
		case "CN_FT_TT3" -> segregateOpenning ? new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime3()) : new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime3(), TradeTimeConstant.CN_FT_NIGHT_OPENNING);
		case "CN_FT_TT4" -> segregateOpenning ? new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime4()) : new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime4(), TradeTimeConstant.CN_FT_DAY_OPENNING1);
		case "CN_FT_TT5" -> segregateOpenning ? new PeriodHelper(numbersOfMinPerPeriod, new CnFtIndexTradeTime()) : new PeriodHelper(numbersOfMinPerPeriod, new CnFtIndexTradeTime(), TradeTimeConstant.CN_FT_DAY_OPENNING2);
		case "CN_FT_TT6" -> segregateOpenning ? new PeriodHelper(numbersOfMinPerPeriod, new CnFtBondTradeTime()) : new PeriodHelper(numbersOfMinPerPeriod, new CnFtBondTradeTime(), TradeTimeConstant.CN_FT_DAY_OPENNING2);
		default -> GENERIC_HELPER;
		};
		getHelperCache(segregateOpenning).put(cd.getTradeTimeType(), numbersOfMinPerPeriod, helper);
		return helper;
	}
	
	private Table<String, Integer, PeriodHelper> getHelperCache(boolean segregateOpenning) {
		return segregateOpenning ? helperCache : helperCache2;
	}
	
	private ContractDefinition findDefinition(ContractField contract) {
		for(ContractDefinition contractDef : contractDefs) {
			if(contractDef.getSymbolPattern().matcher(contract.getThirdPartyId()).matches()) {
				return contractDef;
			}
		}
		
		return null;
	}
}
