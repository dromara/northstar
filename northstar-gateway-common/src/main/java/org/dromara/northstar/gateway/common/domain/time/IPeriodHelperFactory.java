package org.dromara.northstar.gateway.common.domain.time;

import org.dromara.northstar.gateway.model.ContractDefinition;

public interface IPeriodHelperFactory {

	default PeriodHelper newInstance(int numbersOfMinPerPeriod, boolean exclusiveOpenning, ContractDefinition contractDef) {
		return new PeriodHelper(1, new GenericTradeTime());
	}
}
