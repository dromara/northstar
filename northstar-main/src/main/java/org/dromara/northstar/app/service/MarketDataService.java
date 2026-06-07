package org.dromara.northstar.app.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.event.KlineDispatcher;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.kline.BarMerger;
import org.dromara.northstar.kline.BarMergerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;

@Slf4j
@Service
public class MarketDataService {
	
	@Autowired
	private MarketCenter marketCenter;
	
	@Autowired
	private KlineDispatcher klineDispatcher;
	
	@Autowired
	private IMarketDataRepository mdRepo;
	
	private Set<ExchangeEnum> cnFutEx = Set.of(ExchangeEnum.SHFE, 
			ExchangeEnum.CZCE, ExchangeEnum.DCE, ExchangeEnum.CFFEX, ExchangeEnum.INE);
	
	private static final LocalDate SINCE_DATE = LocalDate.of(2021, 1, 1);
	private static final LocalDate SINCE_DATE2 = LocalDate.of(2010, 1, 1);
	
	public List<Bar> getRangeBars(String contractId, Interval interval, Long fromTime, Long toTime){
		IContract ic = marketCenter.getContract(Identifier.of(contractId));
		Contract contract = ic.contract();
		
		int dayDif = switch (interval) {
			case M1 -> 1;
			case M3 -> 3;
			case M5 -> 5;
			case M10 -> 10;
			case M15 -> 15;
			case M30 -> 30;
			case H1 -> 60;
			case H2 -> 120;
			case H4 -> 240;
			case D1,WK1 -> 300;
			default -> throw new IllegalArgumentException("不支持的周期类型：" + interval);
		};
		
		LocalDateTime toDateTime = CommonUtils.millsToLocalDateTime(Optional.ofNullable(toTime).orElse(System.currentTimeMillis()));
		LocalDateTime fromDateTime = fromTime == null ? toDateTime.minusDays(dayDif) : CommonUtils.millsToLocalDateTime(fromTime);
		
		if(toDateTime.toLocalDate().isBefore(SINCE_DATE) && cnFutEx.contains(contract.exchange())) {
			log.warn("国内期货行情历史回溯不能早于2021年1月1日，当前查询范围：{} -> {}", fromDateTime, toDateTime);
			return Collections.emptyList();
		} else if(toDateTime.toLocalDate().isBefore(SINCE_DATE2)) {
			log.warn("行情历史回溯不能早于2010年1月1日，当前查询范围：{} -> {}", fromDateTime, toDateTime);
			return Collections.emptyList();
		}
		log.info("查询历史数据，合约{}，周期{}，{} -> {}", contractId, interval.getAlias(), fromDateTime, toDateTime);
		
		
		IDataSource dataSource = contract.contractDefinition().dataSource();
		if(dataSource == null) {
			return Collections.emptyList();
		}
		
		List<Bar> bars = new LinkedList<>(dataSource.getData(contract, interval, fromDateTime.minusDays(1).toLocalDate(), toDateTime.plusDays(3).toLocalDate()).reversed());
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
				.filter(bar -> bar.actionTimestamp() <= CommonUtils.localDateTimeToMills(toDateTime))
				.filter(bar -> bar.actionTimestamp() > CommonUtils.localDateTimeToMills(fromDateTime))
				.toList();
	}
	
	public void subscribe(List<String> contractIds, List<Interval> intervals) {
		log.info("订阅K线，合约 {}，周期 {}", JSON.toJSONString(contractIds), JSON.toJSONString(intervals.stream().map(Interval::getAlias).toList()));
		klineDispatcher.unregisterAll();
		contractIds.forEach(contractId -> {
			final Contract contract = marketCenter.getContract(Identifier.of(contractId)).contract();
			for(Interval barInterval : intervals) {
				klineDispatcher.register(contract, barInterval);
			}
		});
	}
	
	public List<Tick> getActiveTicks() {
		return marketCenter.latestTicks()
				.stream()
				.sorted((a, b) -> a.contract().unifiedSymbol().compareTo(b.contract().unifiedSymbol()))
				.toList();
	}
	
}
