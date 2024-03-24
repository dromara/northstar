package org.dromara.northstar.data.jdbc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.data.jdbc.entity.BarDO;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IContractManager;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 本地行情数据仓库
 * @auth KevinHuangwl
 */

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
	public List<Bar> loadBars(IContract contract, LocalDate startDate, LocalDate endDate) {
		List<Bar> resultList = new ArrayList<>();
		LocalDate date = startDate;
		while(!date.isAfter(endDate)) {
			resultList.addAll(findBarData(date, contract.contract().unifiedSymbol()));
			date = date.plusDays(1);
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
			return dataServiceDelegate.getDailyData(contract.contract(), startDate, endDate);
		} catch (Exception e) {
			log.error("{}", e.getMessage());
			return Collections.emptyList();
		}
	}
	
}
