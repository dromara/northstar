package org.dromara.northstar.gateway.mktdata;

import java.time.LocalDate;
import java.util.List;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.ResultSet;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.springframework.util.Assert;

/**
 * 数据源处理
 * 负责把数据源的数据做封装，以及统一适配
 * @auth KevinHuangwl
 */

public class NorthstarDataSource implements IDataSource{
	
	private QuantitDataServiceManager dataService;
	
	public NorthstarDataSource(QuantitDataServiceManager dataService) {
		this.dataService = dataService;
	}
	
	@Override
	public List<Bar> getMinutelyData(Contract contract, LocalDate startDate, LocalDate endDate) {
		return convertMinuteData(dataService.getMinutelyData(contract.unifiedSymbol(), startDate, endDate), contract);
	}

	@Override
	public List<Bar> getQuarterlyData(Contract contract, LocalDate startDate, LocalDate endDate) {
		return convertMinuteData(dataService.getQuarterlyData(contract.unifiedSymbol(), startDate, endDate), contract);
	}

	@Override
	public List<Bar> getHourlyData(Contract contract, LocalDate startDate, LocalDate endDate) {
		return convertMinuteData(dataService.getHourlyData(contract.unifiedSymbol(), startDate, endDate), contract);
	}
	
	private List<Bar> convertMinuteData(ResultSet rs, Contract contract){
		return rs.toJSONList().stream()
				.map(json -> Bar.builder()
						.actionDay(null)
						.actionTime(null)
						.actionTimestamp(0)
						.tradingDay(null)
						.channelType(contract.channelType())
						.contract(contract)
						.gatewayId(contract.gatewayId())
						.openPrice(0)
						.closePrice(0)
						.highPrice(0)
						.lowPrice(0)
						.openInterest(0)
						.openInterestDelta(0)
						.volume(0)
						.volumeDelta(0)
						.turnover(0)
						.turnoverDelta(0)
						.preClosePrice(0)
						.preOpenInterest(0)
						.preSettlePrice(0)
						.build())
				.toList();
	}

	@Override
	public List<Bar> getDailyData(Contract contract, LocalDate startDate, LocalDate endDate) {
		return dataService.getDailyData(contract.unifiedSymbol(), startDate, endDate)
				.toJSONList()
				.stream()
				.map(json -> Bar.builder()
						.actionDay(null)
						.actionTime(null)
						.actionTimestamp(0)
						.tradingDay(null)
						.channelType(contract.channelType())
						.contract(contract)
						.gatewayId(contract.gatewayId())
						.openPrice(0)
						.closePrice(0)
						.highPrice(0)
						.lowPrice(0)
						.openInterest(0)
						.openInterestDelta(0)
						.volume(0)
						.volumeDelta(0)
						.turnover(0)
						.turnoverDelta(0)
						.preClosePrice(0)
						.preOpenInterest(0)
						.preSettlePrice(0)
						.build())
				.toList();
	}

	@Override
	public List<LocalDate> getHolidays(ChannelType channelType, LocalDate startDate, LocalDate endDate) {
		Assert.isTrue(channelType == ChannelType.CTP, "只能查询CTP的交易日");
		Assert.isTrue(startDate.getYear() == endDate.getYear(), "只能查询同一年的交易日");
		return dataService.getCalendarCN(startDate.getYear())
				.toJSONList()
				.stream()
				.map(json -> LocalDate.now())
				.filter(date -> true)
				.toList();
	}

	@Override
	public List<Contract> getAllContracts() {
		return dataService.getAllFutureContracts()
				.toJSONList()
				.stream()
				.map(json -> Contract.builder()
						
						.build())
				.toList();
		
	}

}
