package tech.quantit.northstar.gateway.api.domain.time;

import tech.quantit.northstar.gateway.api.domain.contract.ContractDefinition;

public interface IPeriodHelperFactory {

	PeriodHelper newInstance(int numbersOfMinPerPeriod, boolean exclusiveOpenning, ContractDefinition contractDef);
}
