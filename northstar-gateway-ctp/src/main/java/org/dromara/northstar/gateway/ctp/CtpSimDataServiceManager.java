package org.dromara.northstar.gateway.ctp;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.dromara.northstar.common.IDataServiceManager;
import org.dromara.northstar.common.utils.MarketDateTimeUtil;
import org.springframework.web.client.RestTemplate;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

public class CtpSimDataServiceManager extends CtpDataServiceManager implements IDataServiceManager{

	public CtpSimDataServiceManager(String baseUrl, String secret, RestTemplate restTemplate,
			MarketDateTimeUtil dtUtil) {
		super(baseUrl, secret, restTemplate, dtUtil);
	}

	@Override
	public List<BarField> getMinutelyData(ContractField contract, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<BarField> getQuarterlyData(ContractField contract, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<BarField> getHourlyData(ContractField contract, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<BarField> getDailyData(ContractField contract, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<ExchangeEnum> getUserAvailableExchanges() {
		return Collections.emptyList();
	}

}
