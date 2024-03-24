package org.dromara.northstar.gateway.mktdata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.model.ResultSet;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.utils.CommonUtils;
import org.springframework.util.Assert;

import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

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
						.actionDay(LocalDate.parse(json.getString("trade_time").substring(0, 10)))
						.actionTime(LocalTime.parse(json.getString("trade_time").substring(11)))
						.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.parse(json.getString("trade_time"), DateTimeConstant.DT_FORMAT_FORMATTER)))
						.tradingDay(LocalDate.parse(json.getString("trading_day")))
						.channelType(contract.channelType())
						.contract(contract)
						.gatewayId(contract.gatewayId())
						.openPrice(indexContractPriceEnhance(contract, json.getDoubleValue("open")))
						.closePrice(indexContractPriceEnhance(contract, json.getDoubleValue("close")))
						.highPrice(indexContractPriceEnhance(contract, json.getDoubleValue("high")))
						.lowPrice(indexContractPriceEnhance(contract, json.getDoubleValue("low")))
						.openInterest(json.getDoubleValue("oi"))
						.openInterestDelta(json.getDoubleValue("oi_delta"))
						.volume(json.getLongValue("vol"))
						.volumeDelta(json.getLongValue("vol_delta"))
						.turnover(json.getDoubleValue("amount"))
						.turnoverDelta(json.getDoubleValue("amount_delta"))
						.preOpenInterest(json.getDoubleValue("pre_oi"))
						.build())
				.toList();
	}
	
	private double indexContractPriceEnhance(Contract contract, double price) {
		if(!contract.symbol().contains(Constants.INDEX_SUFFIX)) {
			return price;
		}
		int numOfPriceTickInPrice = (int)(price * 1000) / (int)(contract.priceTick() * 1000);
		return numOfPriceTickInPrice * contract.priceTick();
	}

	@Override
	public List<Bar> getDailyData(Contract contract, LocalDate startDate, LocalDate endDate) {
		return dataService.getDailyData(contract.unifiedSymbol(), startDate, endDate)
				.toJSONList()
				.stream()
				.map(json -> Bar.builder()
						.actionDay(LocalDate.parse(json.getString("trade_date"), DateTimeConstant.D_FORMAT_INT_FORMATTER))
						.tradingDay(LocalDate.parse(json.getString("trade_date"), DateTimeConstant.D_FORMAT_INT_FORMATTER))
						.channelType(contract.channelType())
						.contract(contract)
						.gatewayId(contract.gatewayId())
						.openPrice(indexContractPriceEnhance(contract, json.getDoubleValue("open")))
						.closePrice(indexContractPriceEnhance(contract, json.getDoubleValue("close")))
						.highPrice(indexContractPriceEnhance(contract, json.getDoubleValue("high")))
						.lowPrice(indexContractPriceEnhance(contract, json.getDoubleValue("low")))
						.openInterest(json.getDoubleValue("oi"))
						.openInterestDelta(json.getDoubleValue("oi_chg"))
						.volume(json.getLongValue("vol"))
						.volumeDelta(json.getLongValue("vol"))
						.turnover(json.getDoubleValue("amount"))
						.turnoverDelta(json.getDoubleValue("amount"))
						.preOpenInterest(json.getDoubleValue("pre_oi"))
						.preClosePrice(indexContractPriceEnhance(contract, json.getDoubleValue("pre_close")))
						.preSettlePrice(json.getDoubleValue("pre_settle"))
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
				.filter(json -> json.getIntValue("is_open") == 0)
				.map(json -> LocalDate.parse(json.getString("date"), DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.toList();
	}

	@Override
	public List<Contract> getAllContracts() {
		return dataService.getAllFutureContracts()
				.toJSONList()
				.stream()
				.map(json -> Contract.builder()
						.unifiedSymbol(json.getString("unifiedSymbol"))
						.symbol(json.getString("unifiedSymbol").replaceAll("@.+$", ""))
						.underlyingSymbol(json.getString("fut_code"))
						.name(json.getString("name"))
						.fullName(json.getString("name"))
						.productClass(ProductClassEnum.valueOf(json.getString("unifiedSymbol").replaceAll("^.+@.+@(.+)$", "$1")))
						.exchange(ExchangeEnum.valueOf(json.getString("exchange")))
						.currency(CurrencyEnum.CNY)
						.multiplier(json.getDoubleValue("multiplier"))
						.priceTick(json.getDoubleValue("price_tick"))
						.lastTradeDate(LocalDate.parse(json.getString("delist_date"), DateTimeConstant.D_FORMAT_INT_FORMATTER))
						.longMarginRatio(0.1)
						.shortMarginRatio(0.1)
						.build())
				.toList();
		
	}

}
