package tech.quantit.northstar.strategy.api.utils.time;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import tech.quantit.northstar.common.model.ContractDefinition;
import tech.quantit.northstar.common.utils.ContractDefinitionReader;
import tech.quantit.northstar.strategy.api.utils.time.trade.CnFtBondTradeTime;
import tech.quantit.northstar.strategy.api.utils.time.trade.CnFtComTradeTime1;
import tech.quantit.northstar.strategy.api.utils.time.trade.CnFtComTradeTime2;
import tech.quantit.northstar.strategy.api.utils.time.trade.CnFtComTradeTime3;
import tech.quantit.northstar.strategy.api.utils.time.trade.CnFtComTradeTime4;
import tech.quantit.northstar.strategy.api.utils.time.trade.CnFtIndexTradeTime;
import tech.quantit.northstar.strategy.api.utils.time.trade.TradeTimeConstant;
import xyz.redtorch.pb.CoreField.ContractField;

public class PeriodHelperFactory {
	private static List<ContractDefinition> contractDefs;
	static {
		try {
			String fileName = "ContractDefinition.csv";
			String tempPath = System.getProperty("java.io.tmpdir") + "Northstar_" + System.currentTimeMillis();
			String tempFilePath = tempPath + File.separator + fileName;
			Resource resource = new DefaultResourceLoader().getResource("classpath:" + fileName);
			File tempFile = new File(tempFilePath);
			FileUtils.forceMkdirParent(tempFile);
			try (FileOutputStream fos = new FileOutputStream(tempFile)) {
				IOUtils.copy(resource.getInputStream(), fos);
			}
			
			ContractDefinitionReader reader = new ContractDefinitionReader();
			contractDefs = reader.load(tempFile);
		} catch(Exception e) {
			throw new Error(e);
		}
	}
	
	private PeriodHelperFactory() {}

	public static PeriodHelper newInstance(int numbersOfMinPerPeriod, boolean segregateOpenning, ContractField contract) {
		ContractDefinition cd = findDefinition(contract);
		if(Objects.isNull(cd)) {
			return null;
		}
		return switch(cd.getTradeTimeType()) {
		case "CN_FT_TT1" -> segregateOpenning ? new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime1()) : new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime1(), TradeTimeConstant.CN_FT_NIGHT_OPENNING);
		case "CN_FT_TT2" -> segregateOpenning ? new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime2()) : new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime2(), TradeTimeConstant.CN_FT_NIGHT_OPENNING);
		case "CN_FT_TT3" -> segregateOpenning ? new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime3()) : new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime3(), TradeTimeConstant.CN_FT_NIGHT_OPENNING);
		case "CN_FT_TT4" -> segregateOpenning ? new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime4()) : new PeriodHelper(numbersOfMinPerPeriod, new CnFtComTradeTime4(), TradeTimeConstant.CN_FT_DAY_OPENNING1);
		case "CN_FT_TT5" -> segregateOpenning ? new PeriodHelper(numbersOfMinPerPeriod, new CnFtIndexTradeTime()) : new PeriodHelper(numbersOfMinPerPeriod, new CnFtIndexTradeTime(), TradeTimeConstant.CN_FT_DAY_OPENNING2);
		case "CN_FT_TT6" -> segregateOpenning ? new PeriodHelper(numbersOfMinPerPeriod, new CnFtBondTradeTime()) : new PeriodHelper(numbersOfMinPerPeriod, new CnFtBondTradeTime(), TradeTimeConstant.CN_FT_DAY_OPENNING2);
		default -> null;
		};
	}
	
	private static ContractDefinition findDefinition(ContractField contract) {
		for(ContractDefinition contractDef : contractDefs) {
			if(contractDef.getSymbolPattern().matcher(contract.getThirdPartyId()).matches()) {
				return contractDef;
			}
		}
		
		return null;
	}
}
