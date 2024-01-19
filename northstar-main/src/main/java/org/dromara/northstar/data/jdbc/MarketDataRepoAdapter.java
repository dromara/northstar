package org.dromara.northstar.data.jdbc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.data.jdbc.entity.BarDO;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IContractManager;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.BarField;

@Slf4j
public class MarketDataRepoAdapter implements IMarketDataRepository{

	private MarketDataRepository delegate;
	
	private IContractManager contractMgr;
	
	public MarketDataRepoAdapter(MarketDataRepository delegate, IContractManager contractMgr) {
		this.delegate = delegate;
		this.contractMgr = contractMgr;
	}
	
	@Override
	public void insert(Bar bar) {
		delegate.save(BarDO.builder()
				.unifiedSymbol(bar.contract().unifiedSymbol())
				.tradingDay(bar.tradingDay().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.expiredAt(CommonUtils.localDateTimeToMills(LocalDateTime.of(bar.tradingDay(), LocalTime.of(20, 0))))
				.barData(bar.toBarField().toByteArray())
				.build());
	}

	@Override
	public List<Bar> loadBars(IContract contract, LocalDate startDate, LocalDate endDate0) {
		log.debug("加载 [{}] 历史行情数据：{} -> {}", contract.contract().unifiedSymbol(), startDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), endDate0.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
		LocalDate today = LocalDate.now();
		LocalDate endDate = today.isAfter(endDate0) ? endDate0 : today;
		LinkedList<Bar> resultList = new LinkedList<>();
		IDataSource dataServiceDelegate = contract.dataSource();
		if(dataServiceDelegate == null) {
			log.warn("合约[{}] 没有提供历史数据源，无法加载历史数据", contract.name());
			return List.of();
		}
		Contract cf = contract.contract();
		if(endDate.isAfter(startDate)) {
			List<Bar> list = dataServiceDelegate.getMinutelyData(cf.unifiedSymbol(), startDate, endDate)
					.stream()
					.sorted((a, b) -> a.actionTimestamp() < b.actionTimestamp() ? -1 : 1)
					.toList();
			resultList.addAll(list);
		}
		if(today.isAfter(endDate0))	return resultList; 
		
		LocalDate localQueryDate = today;
		if(resultList.isEmpty()) {
			while(resultList.isEmpty() && endDate0.isAfter(localQueryDate)) {
				resultList.addAll(findBarData(localQueryDate, cf.unifiedSymbol()));
				localQueryDate = localQueryDate.plusDays(1);
			}
		} else {			
			if(Objects.equals(resultList.peekLast().tradingDay(), today)
					|| today.getDayOfWeek().getValue() > 5) {
				do {					
					localQueryDate = localQueryDate.plusDays(1);
				} while(localQueryDate.getDayOfWeek().getValue() > 5);
				resultList.addAll(findBarData(localQueryDate, cf.unifiedSymbol()));
			} else {
				resultList.addAll(findBarData(localQueryDate, cf.unifiedSymbol()));
			}
		}
		
		return resultList;
	}
	
	private List<Bar> findBarData(LocalDate date, String unifiedSymbol){
		String tradingDay = date.format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
		log.debug("加载 [{}] 本地行情数据：{}", unifiedSymbol, tradingDay);
		try {
			return delegate.findByUnifiedSymbolAndTradingDay(unifiedSymbol, tradingDay)
					.stream()
					.map(BarDO::getBarData)
					.map(this::convertFrom)
					.filter(Objects::nonNull)
					.map(bar -> Bar.of(bar, contractMgr.getContract(ChannelType.valueOf(bar.getChannelType()), bar.getUnifiedSymbol()).contract()))
					.toList();
		} catch (Exception e) {
			log.error("数据转换异常", e);
			return Collections.emptyList();
		}
	}
	
	private BarField convertFrom(byte[] data) {
		try {
			return BarField.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			log.warn("", e);
			return null;
		}
	}
	
	@Override
	public List<Bar> loadDailyBars(IContract contract, LocalDate startDate, LocalDate endDate) {
		IDataSource dataServiceDelegate = contract.dataSource();
		try {
			return dataServiceDelegate.getDailyData(contract.contract().unifiedSymbol(), startDate, endDate);
		} catch (Exception e) {
			log.error("{}", e.getMessage());
			return Collections.emptyList();
		}
	}
	
}
