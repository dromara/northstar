package org.dromara.northstar.app.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
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
	
	public List<BarVO> getRangeBars(String contractId, Interval interval, Long fromTime, Long toTime){
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
		
		LocalDate toDate = CommonUtils.millsToLocalDateTime(Optional.ofNullable(toTime).orElse(System.currentTimeMillis())).toLocalDate();
		LocalDate fromDate = fromTime == null ? toDate.minusDays(dayDif) : CommonUtils.millsToLocalDateTime(fromTime).toLocalDate();
		
		if(toDate.isBefore(SINCE_DATE) && cnFutEx.contains(contract.exchange())) {
			log.warn("国内期货行情历史回溯不能早于2021年1月1日，当前查询范围：{} -> {}", fromDate, toDate);
			return Collections.emptyList();
		} else if(toDate.isBefore(SINCE_DATE2)) {
			log.warn("行情历史回溯不能早于2010年1月1日，当前查询范围：{} -> {}", fromDate, toDate);
			return Collections.emptyList();
		}
		log.info("查询历史数据，合约{}，周期{}，{} -> {}", contractId, interval.getAlias(), fromDate, toDate);
		
		
		IDataSource dataSource = contract.contractDefinition().dataSource();
		if(dataSource == null) {
			return Collections.emptyList();
		}
		
		List<Bar> bars = new LinkedList<>(dataSource.getData(contract, interval, fromDate, toDate).reversed());
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
				.filter(bar -> bar.actionTimestamp() < Optional.ofNullable(toTime).orElse(System.currentTimeMillis()))
				.filter(bar -> bar.actionTimestamp() > Optional.ofNullable(fromTime).orElse(0L))
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
	
	public void subscribe(List<String> contractIds, List<Interval> intervals) {
		log.info("订阅K线，合约 {}，周期 {}", JSON.toJSONString(contractIds), JSON.toJSONString(intervals));
		klineDispatcher.unregisterAll();
		contractIds.forEach(contractId -> {
			final Contract contract = marketCenter.getContract(Identifier.of(contractId)).contract();
			for(Interval barInterval : intervals) {
				klineDispatcher.register(contract, barInterval);
			}
		});
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
