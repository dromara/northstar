package tech.quantit.northstar.gateway.api.domain.time;

import xyz.redtorch.pb.CoreField.ContractField;

public interface IPeriodHelperFactory {

	PeriodHelper newInstance(int numbersOfMinPerPeriod, boolean exclusiveOpenning, ContractField contract);
}
