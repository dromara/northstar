package org.dromara.northstar.gateway.sim;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.IDataServiceManager;
import org.dromara.northstar.gateway.mktdata.MinuteBarGenerator;
import org.dromara.northstar.gateway.sim.market.SimTickGenerator;
import org.dromara.northstar.gateway.time.GenericTradeTime;
import org.springframework.util.Assert;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

public class SimMockDataManager implements IDataServiceManager{
	
	private Map<String, SimTickGenerator> tickGenMap;
	
	public SimMockDataManager(Map<String, SimTickGenerator> tickGenMap) {
		this.tickGenMap = tickGenMap;
	}

	@Override
	public List<BarField> getMinutelyData(ContractField contract, LocalDate startDate, LocalDate endDate) {
		SimTickGenerator tickGen = tickGenMap.get(contract.getUnifiedSymbol());
		Assert.notNull(tickGen, contract.getUnifiedSymbol() + "没有找到Tick生成器");
		List<BarField> resultList = new ArrayList<>(60*24);
		MinuteBarGenerator barGen = new MinuteBarGenerator(contract, new GenericTradeTime(), resultList::add, true);
		LocalDateTime cutoff = LocalDateTime.of(endDate, LocalTime.now());
		LocalDateTime ldt = cutoff.minusDays(1);
		while(ldt.isBefore(cutoff)) {
			TickField tick = tickGen.generateNextTick(ldt);
			barGen.update(tick);
			ldt = ldt.plusSeconds(1);
		}
		return resultList;
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
	public List<LocalDate> getHolidays(ExchangeEnum exchange, LocalDate startDate, LocalDate endDate) {
		return Collections.emptyList();
	}

	@Override
	public List<ContractField> getAllContracts(ExchangeEnum exchange) {
		throw new UnsupportedOperationException();
	}

}
