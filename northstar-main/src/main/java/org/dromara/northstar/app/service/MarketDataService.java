package org.dromara.northstar.app.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.BarVO;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.TickVO;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.event.BarAggregationHandler;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.mktdata.DataSourceDataLoader;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.support.utils.bar.BarMerger;
import org.dromara.northstar.support.utils.bar.BarMergerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

@Slf4j
@Service
public class MarketDataService {
	
	@Autowired
	private MarketCenter marketCenter;
	
	@Autowired
	private BarAggregationHandler barAggHandler;
	
	@Autowired
	private IMarketDataRepository mdRepo;
	
	private static final LocalDate SINCE_DATE = LocalDate.of(2021, 1, 1);
	
	public List<BarVO> getRangeBars(String contractId, Interval interval, long fromTime, long toTime){
		LocalDate toDate = CommonUtils.millsToLocalDateTime(toTime).toLocalDate();
		LocalDate fromDate = CommonUtils.millsToLocalDateTime(fromTime).toLocalDate();
		if(!contractId.contains(ProductClassEnum.EQUITY.toString())) {
			fromDate = CommonUtils.millsToLocalDateTime(Math.max(fromTime, CommonUtils.localDateTimeToMills(LocalDateTime.of(SINCE_DATE, LocalTime.of(0, 0))))).toLocalDate();
		}
		if(toDate.isBefore(SINCE_DATE) && !contractId.contains(ProductClassEnum.EQUITY.toString())) {
			log.warn("历史回溯不能早于2021年1月1日，当前查询范围：{} -> {}", fromDate, toDate);
			return Collections.emptyList();
		}
		log.info("查询历史数据，合约{}，周期{}，{} -> {}", contractId, interval.getAlias(), fromDate, toDate);
		IContract ic = marketCenter.getContract(Identifier.of(contractId));
		Contract contract = ic.contract();
		
		IDataSource dataSource = contract.contractDefinition().dataSource();
		if(dataSource == null) {
			return Collections.emptyList();
		}
		
		List<Bar> bars = smartLoad(contract, interval, dataSource, fromDate, toDate);
		List<Bar> mergedBars = new LinkedList<>();
		// 本地仅加载最近的数据
		List<Bar> data = mdRepo.loadBars(ic, LocalDate.now(), LocalDate.now().plusDays(3));
		if(!bars.isEmpty()) {
			long endTime = bars.getLast().actionTimestamp();
			data = data.stream().filter(b -> b.actionTimestamp() > endTime).toList();
		}
		if (interval != Interval.M1) {
			// 合成处理
			BarMerger merger = BarMergerFactory.newInstance(interval, contract);
			merger.addListener(mergedBars::add);
			bars.stream().forEach(merger::onBar);
			data.stream().forEach(merger::onBar);
		} else {
			bars.addAll(data);
			mergedBars = bars;
		}
		
		return mergedBars.stream()
				.filter(bar -> bar.actionTimestamp() < toTime)
				.filter(bar -> bar.actionTimestamp() > fromTime)
				.map(bar -> BarVO.builder()
						.timestamp(bar.actionTimestamp())
						.open(bar.openPrice())
						.close(bar.closePrice())
						.high(bar.highPrice())
						.low(bar.lowPrice())
						.volume(bar.volumeDelta())
						.oi(bar.openInterest())
						.tradingDay(bar.tradingDay().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
						.interval(interval.getAlias())
						.build())
				.toList();
	}
	
	public List<Bar> smartLoad(Contract contract, Interval interval, IDataSource ds, LocalDate loadStartDate, LocalDate loadEndDate) {
		DataSourceDataLoader loader = new DataSourceDataLoader(ds);
		LinkedList<Bar> aggResult = new LinkedList<>();
		if(interval.getMinutes() < 15) {
			loader.loadMinutelyData(contract, loadStartDate, loadEndDate, list -> aggResult.addAll(0, list));
		} else if (interval.getMinutes() < 60) {
			loader.loadQuarterlyData(contract, loadStartDate, loadEndDate, list -> aggResult.addAll(0, list));
		} else if (interval.getMinutes() < 24 * 60) {
			loader.loadHourlyData(contract, loadStartDate, loadEndDate, list -> aggResult.addAll(0, list));
		} else {
			loader.loadDailyData(contract, loadStartDate, loadEndDate, list -> aggResult.addAll(0, list));
		}
		return aggResult.reversed();
	}
	
	public void subscribe(List<String> contractIds) {
		log.info("订阅K线，合约 {}", JSON.toJSONString(contractIds));
		
		Set<Contract> newSubscribedContracts = new HashSet<>();
		contractIds.forEach(contractId -> {
			final Contract contract = marketCenter.getContract(Identifier.of(contractId)).contract();
			newSubscribedContracts.add(contract);
			for(Interval barInterval : Interval.values()) {
				barAggHandler.register(contract, barInterval, BarMergerFactory.newInstance(barInterval, contract));
			}
		});
		
		unsubscribedContracts(newSubscribedContracts, barAggHandler.registeredContracts()).forEach(contract -> {
			for(Interval barInterval : Interval.values()) {				
				barAggHandler.unregister(contract,  barInterval);
			}
		});
	}
	
	private Set<Contract> unsubscribedContracts(Set<Contract> newSet, Set<Contract> oldSet){
		return oldSet.stream()
				.filter(c -> !newSet.contains(c))
				.collect(java.util.stream.Collectors.toSet());
	}
	
	public List<TickVO> getActiveTicks() {
		return marketCenter.latestTicks()
				.stream()
				.sorted((a, b) -> a.contract().unifiedSymbol().compareTo(b.contract().unifiedSymbol()))
				.map(tick -> TickVO.builder()
						.name(tick.contract().name())
						.symbol(tick.contract().symbol())
						.lastPrice(tick.lastPrice())
						.priceChange(tick.lastPrice() - tick.preSettlePrice())
						.openInterest(tick.openInterest())
						.openInterestDelta(tick.openInterestDelta())
						.volume(tick.volume())
						.timestamp(tick.actionTimestamp())
						.precision(tick.contract().pricePrecision())
						.channel(tick.channelType())
						.build())
				.toList();
	}
	
}
