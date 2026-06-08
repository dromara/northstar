package org.dromara.northstar.gateway.mktdata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.exception.QuantitDataSourceException;
import org.dromara.northstar.common.exception.UnauthorizedException;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.datasource.dto.DataSourceMessage.AggBarDTO;
import org.dromara.northstar.datasource.dto.DataSourceMessage.ContractDTO;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

/**
 * 数据源处理
 * 负责把数据源的数据做封装，以及统一适配
 * @auth KevinHuangwl
 */
@Slf4j
public class NorthstarDataSource implements IDataSource{
	
	private QuantitDataServiceManager dataMgr;
	
	public static final int CN_FUTURES = 1;
	public static final int CN_STOCKS = 2;
	public static final int US_STOCKS = 4;
	public static final int HK_STOCKS = 8;
	public static final int FOREX = 16;
	public static final int CRYPTO = 32;
	public static final int US_ETF = 64;
	
	private int defaultMode = CN_FUTURES | CN_STOCKS | CRYPTO; // | HK_STOCKS | US_STOCKS | FOREX | US_ETF;
	
	public NorthstarDataSource(QuantitDataServiceManager dataMgr) {
		this.dataMgr = dataMgr;
	}
	
	public NorthstarDataSource(QuantitDataServiceManager dataMgr, int subscription) {
		this.dataMgr = dataMgr;
		this.defaultMode = subscription;
	}
	
	private static final Pattern MESSAGE_PATTERN = Pattern.compile(
        "[\"']?message[\"']?\\s*:\\s*[\"'](.*?)[\"'](?=,|\\s*}|\\s*$)",
        Pattern.CASE_INSENSITIVE
    );
    
    public static String extractMessage(String text) {
        if (text == null) return null;
        
        Matcher matcher = MESSAGE_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : "请查看日志的具体错误信息";
    }
	
	@Override
	public List<Bar> getData(Contract contract, Interval interval, LocalDate startDate, LocalDate endDate) {
		try {
			return enhanceData(dataMgr.getData(contract.unifiedSymbol(), interval, startDate, endDate), contract, interval);
		} catch (HttpClientErrorException e) {
			if(e.getStatusCode() == HttpStatus.FORBIDDEN) {
				throw new UnauthorizedException(contract.name());
			}
			if(e.getStatusCode() == HttpStatus.BAD_REQUEST) {
				log.warn("{}", e.getMessage());
				throw new IllegalArgumentException(extractMessage(e.getMessage()));
			}
			throw new QuantitDataSourceException(e);
		} catch (Exception e) {
			throw new QuantitDataSourceException(e);
		}
	}
	
	private Pattern bondPtn = Pattern.compile("(TS|TF|T|TL)\\d{4}");
	private Set<ExchangeEnum> futEx = Set.of(ExchangeEnum.CFFEX, ExchangeEnum.INE, ExchangeEnum.DCE, ExchangeEnum.SHFE, ExchangeEnum.CZCE, ExchangeEnum.GFEX);
	private Set<Interval> enhancingIntervals = Set.of(Interval.D1, Interval.WK1);
	
	private long timestampEnhancement(AggBarDTO dto, Contract contract, Interval interval) {
		// 增强修正期货日线收盘时间 
		if(enhancingIntervals.contains(interval) && futEx.contains(contract.exchange())) {
			LocalDateTime adjustDT = null;
			LocalDate tradingDay = LocalDate.parse(dto.getTradingDay());
			if(bondPtn.matcher(contract.symbol()).matches()) {
				adjustDT = LocalDateTime.of(tradingDay, LocalTime.of(15, 15));
			} else {
				adjustDT = LocalDateTime.of(tradingDay, LocalTime.of(15, 0));
			}
			return CommonUtils.localDateTimeToMills(adjustDT);
		}
		return dto.getTimestamp();
	}
	
	private List<Bar> enhanceData(List<AggBarDTO> data, Contract contract, Interval interval){
		return data.stream()
				.map(dto -> Bar.builder()
						.actionDay(CommonUtils.millsToLocalDateTime(timestampEnhancement(dto, contract, interval)).toLocalDate())
						.actionTime(CommonUtils.millsToLocalDateTime(timestampEnhancement(dto, contract, interval)).toLocalTime())
						.actionTimestamp(timestampEnhancement(dto, contract, interval))
						.tradingDay(LocalDate.parse(dto.getTradingDay()))
						.channelType(contract.channelType())
						.gatewayId(contract.channelType() + "")
						.contract(contract)
						.openPrice(indexContractPriceEnhance(contract, dto.getOpen()))
						.closePrice(indexContractPriceEnhance(contract, dto.getClose()))
						.highPrice(indexContractPriceEnhance(contract, dto.getHigh()))
						.lowPrice(indexContractPriceEnhance(contract, dto.getLow()))
						.openInterest(dto.getOpenInterest())
						.openInterestDelta((int)dto.getOpenInterestDelta())
						.volume(dto.getVolume())
						.volumeDelta(dto.getVolumeDelta())
						.turnover(dto.getTurnover())
						.turnoverDelta(dto.getTurnoverDelta())
						.preOpenInterest(dto.getPreOI())
						.interval(interval)
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
	public List<LocalDate> getHolidays(ChannelType channelType, LocalDate startDate, LocalDate endDate) {
		Assert.isTrue(channelType == ChannelType.CTP, "只能查询CTP的交易日");
		Assert.isTrue(startDate.getYear() == endDate.getYear(), "只能查询同一年的交易日");
		try {
			return dataMgr.getHolidays(startDate.getYear())
					.stream()
					.filter(dto -> dto.getIsCnHoliday())
					.map(dto -> LocalDate.parse(dto.getCalendarDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER))
					.filter(date -> !date.isBefore(startDate))
					.filter(date -> !date.isAfter(endDate))
					.toList();
		} catch (ResourceAccessException e) {
			throw new QuantitDataSourceException(e);
		}
	}
	
	private boolean has(int mode) {
		return (defaultMode & mode) > 0;
	}

	@Override
	public List<Contract> getAllContracts() {
		ConcurrentLinkedQueue<Contract> results = new ConcurrentLinkedQueue<>();
		CountDownLatch cdl = new CountDownLatch(Integer.bitCount(defaultMode));
		
		if(has(CN_FUTURES)) {			
			Thread.startVirtualThread(() -> {
				try {
					Function<ContractDTO, Contract> mapper = makeConvertFn(ProductClassEnum.FUTURES, CurrencyEnum.CNY);
					List<Contract> result = dataMgr.getCnFutureContracts().stream().map(mapper).toList();
					results.addAll(result);
					log.info("加载国内期货合约：{}个", result.size());
				} catch(Exception e) {
					log.warn("国内期货合约加载失败：{}", e.getMessage());
				} finally {
					cdl.countDown();
				}
			});
		}
		
//		if(has(US_STOCKS)) {
//			Thread.startVirtualThread(() -> {
//				try {
//					Function<ContractDTO, Contract> mapper = makeConvertFn(ProductClassEnum.EQUITY, CurrencyEnum.USD);
//					List<Contract> result = dataMgr.getUsStocksContracts().stream().map(mapper).toList();
//					results.addAll(result);
//					log.info("加载美股合约：{}个", result.size());
//				} catch(Exception e) {
//					log.warn("美股合约加载失败：{}", e.getMessage());
//				} finally {
//					cdl.countDown();
//				}
//			});
//		}
//		
//		if(has(US_ETF)) {
//			Thread.startVirtualThread(() -> {
//				try {
//					Function<ContractDTO, Contract> mapper = makeConvertFn(ProductClassEnum.ETF, CurrencyEnum.USD);
//					List<Contract> result = dataMgr.getUsETFContracts().stream().map(mapper).toList();
//					results.addAll(result);
//					log.info("加载美股ETF合约：{}个", result.size());
//				} catch(Exception e) {
//					log.warn("美股ETF合约加载失败：{}", e.getMessage());
//				} finally {
//					cdl.countDown();
//				}
//			});
//		}
//		
//		if(has(FOREX)) {
//			Thread.startVirtualThread(() -> {
//				try {
//					Function<ContractDTO, Contract> mapper = makeConvertFn(ProductClassEnum.FOREX, CurrencyEnum.USD);
//					List<Contract> result = dataMgr.getForexContracts().stream().map(mapper).toList();
//					results.addAll(result);
//					log.info("加载外汇合约：{}个", result.size());
//				} catch(Exception e) {
//					log.warn("外汇合约加载失败：{}", e.getMessage());
//				} finally {
//					cdl.countDown();
//				}
//			});
//		}
//		
//		if(has(HK_STOCKS)) {
//			Thread.startVirtualThread(() -> {
//				try {
//					Function<ContractDTO, Contract> mapper = makeConvertFn(ProductClassEnum.EQUITY, CurrencyEnum.HKD);
//					List<Contract> result = dataMgr.getHkStocksContracts().stream().map(mapper).toList();
//					results.addAll(result);
//					log.info("加载港股合约：{}个", result.size());
//				} catch(Exception e) {
//					log.warn("港股合约加载失败：{}", e.getMessage());
//				} finally {
//					cdl.countDown();
//				}
//			});
//		}
		
		if(has(CN_STOCKS)) {
			Thread.startVirtualThread(() -> {
				try {
					Function<ContractDTO, Contract> mapper = makeConvertFn(ProductClassEnum.EQUITY, CurrencyEnum.CNY);
					List<Contract> result = dataMgr.getCnStocksContracts().stream().map(mapper).toList();
					results.addAll(result);
					log.info("加载A股合约：{}个", result.size());
				} catch(Exception e) {
					log.warn("A股合约加载失败：{}", e.getMessage());
				} finally {
					cdl.countDown();
				}
			});
		}
		
		if(has(CRYPTO)) {
			Thread.startVirtualThread(() -> {
				try {
					Function<ContractDTO, Contract> futMapper = makeConvertFn(ProductClassEnum.SWAP, CurrencyEnum.USDT);
					List<Contract> swapResult = dataMgr.getCryptoContracts()
							.stream()
							.map(futMapper)
							.toList();
					results.addAll(swapResult);
					log.info("加载加密货币合约：{}个", swapResult.size());
				} catch(Exception e) {
					log.warn("加密货币合约加载失败：{}", e.getMessage());
				} finally {
					cdl.countDown();
				}
			});
		}
		
		try {
			cdl.await();
		} catch (InterruptedException e) {
			log.warn("中断等待");
		}
		
		return results.stream().toList();
	}
	
	private Function<ContractDTO, Contract> makeConvertFn(ProductClassEnum productClass, CurrencyEnum currency){
		return dto -> Contract.builder()
				.contractId(null)
				.unifiedSymbol(String.format("%s@%s@%s", dto.getSymbol(), dto.getExchange(), productClass))
				.symbol(dto.getSymbol())
				.name(dto.getName())
				.fullName(dto.getName())
				.underlyingSymbol(dto.getUnderlyingSymbol())
				.multiplier(dto.getMultiplier())
				.productClass(productClass)
				.exchange(ExchangeEnum.valueOf(dto.getExchange()))
				.priceTick(Double.valueOf(dto.getPriceTick()))
				.pricePrecision(dto.getPricePrecision())
				.quantityPrecision(dto.getQuantityPrecision())
				.currency(currency)
				.lastTradeDate(LocalDate.parse(dto.getLastTradeDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.onboardingDate(StringUtils.isBlank(dto.getOnboardingDate()) ? null : LocalDate.parse(dto.getOnboardingDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.usingStartTimeInBarTime(dto.getUsingStartTimeInBar())
				.build();
	}

	@Override
	public List<Bar> getData(Contract contract, Interval interval, long startTimeInclusive, long endTimeExclusive) {
    	try {
			return enhanceData(dataMgr.getData(contract.unifiedSymbol(), interval, startTimeInclusive, endTimeExclusive), contract, interval);
		} catch (HttpClientErrorException e) {
			if(e.getStatusCode() == HttpStatus.FORBIDDEN) {
				throw new UnauthorizedException(contract.name());
			}
			if(e.getStatusCode() == HttpStatus.BAD_REQUEST) {
				log.warn("{}", e.getMessage());
				throw new IllegalArgumentException(extractMessage(e.getMessage()));
			}
			throw new QuantitDataSourceException(e);
		} catch (Exception e) {
			throw new QuantitDataSourceException(e);
		}
	}
}
