package org.dromara.northstar.gateway.mktdata;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.gateway.contract.IndexContract;

import lombok.extern.slf4j.Slf4j;

/* 注意，本类的日志输出在logs/DEBUG/MarketData_*.log文件 */
@Slf4j
public class IndexTicker {

	private IndexContract idxContract;
	
	private Consumer<Tick> onTickCallback;
	
	private final Set<Contract> memberContracts;
	
	private ConcurrentHashMap<Contract, Tick> tickMap = new ConcurrentHashMap<>(20);
	
	private long lastTickTimestamp = -1;
	
	private double lastPrice;
	private double highPrice;
	private double lowPrice;
	private double openPrice;
	private long totalVolume;
	private long totalVolumeDelta;
	private double totalOpenInterest;
	private double totalOpenInterestDelta;
	private double totalTurnover;
	private double totalTurnoverDelta;
	private double preClose;
	private double preOpenInterest;
	private double preSettlePrice;
	private double settlePrice;
	
	private Tick lastIdxTick;
	
	public IndexTicker(IndexContract idxContract, Consumer<Tick> onTickCallback) {
		this.idxContract = idxContract;
		this.memberContracts = idxContract.memberContracts().stream().map(c -> c.contract()).collect(Collectors.toSet());
		this.onTickCallback = onTickCallback;
	}
	
	private double activeRate() {
		return (double) tickMap.size() / memberContracts.size();
	}

	public synchronized void update(Tick tick) {
		if(!memberContracts.contains(tick.contract())) {
			log.warn("[{}]指数TICK生成器，无法处理 [{}] 的行情数据", idxContract.contract().unifiedSymbol(), tick.contract().unifiedSymbol());
			return;
		}
		if(log.isTraceEnabled()) {
			log.trace("{}指数合成器收到TICK: {}", idxContract.contract().unifiedSymbol(), tick);
		}
		// 如果有过期的TICK数据(例如不活跃的合约),则并入下个K线
		if (0 < lastTickTimestamp && lastTickTimestamp < tick.actionTimestamp()) {
			boolean isReady = activeRate() > 0.7;
			if(isReady) {					
				final Double zeroD = Constants.ZERO_D;
				final Integer zero = Constants.ZERO;
				//进行运算
				calculate();
				lastIdxTick = Tick.builder()
						.gatewayId(tick.gatewayId())
						.contract(idxContract.contract())
						.actionDay(tick.actionDay())
						.actionTime(tick.actionTime())
						.tradingDay(tick.tradingDay())
						.actionTimestamp(lastTickTimestamp)
						.openPrice(openPrice)
						.highPrice(highPrice)
						.lowPrice(lowPrice)
						.lastPrice(lastPrice)
						.openInterest(totalOpenInterest)
						.openInterestDelta(totalOpenInterestDelta)
						.volume(totalVolume)
						.volumeDelta(totalVolumeDelta)
						.turnover(totalTurnover)
						.turnoverDelta(totalTurnoverDelta)
						.preClosePrice(preClose)
						.preOpenInterest(preOpenInterest)
						.preSettlePrice(preSettlePrice)
						.settlePrice(settlePrice)
						.askPrice(List.of(zeroD,zeroD,zeroD,zeroD,zeroD))
						.bidPrice(List.of(zeroD,zeroD,zeroD,zeroD,zeroD))
						.askVolume(List.of(zero,zero,zero,zero,zero))
						.bidVolume(List.of(zero,zero,zero,zero,zero))
						.type(tick.type())
						.channelType(tick.channelType())
						.build();
				onTickCallback.accept(lastIdxTick);
			} else {
				log.debug("{}因月份数据不足，未达到指数合成条件，忽略指数TICK合成计算：当前合约数[{}]，总合约数[{}]，活跃率[{}]", 
						idxContract.contract().unifiedSymbol(), tickMap.size(), memberContracts.size(), activeRate());
			}
		}
		if(tick.type() == TickType.MARKET_TICK && tick.actionTimestamp() > lastTickTimestamp) {			
			lastTickTimestamp = tick.actionTimestamp();
		}
		// 同一个指数Tick
		tickMap.compute(tick.contract(), (k, v) -> tick);
	}
	
	private void calculate() {
		List<Tick> ticks = tickMap.values().stream().toList();
		RealMatrix priceMat = matrixOf(ticks);
		RealMatrix oiWeight = oiWeighted(priceMat);
		RealMatrix bcResult = new Array2DRowRealMatrix(priceMat.getRowDimension(), priceMat.getColumnDimension());
		for(int i=0; i<priceMat.getRowDimension(); i++) {
			double scalar = oiWeight.getEntry(i, 0);
			bcResult.setRow(i, priceMat.getRowMatrix(i).scalarMultiply(scalar).getRow(0));
		}
		
		preOpenInterest = DoubleStream.of(priceMat.getColumn(13)).sum();
		
		// 合计持仓量
		totalOpenInterest = DoubleStream.of(priceMat.getColumn(0)).sum();
		totalOpenInterestDelta = Objects.nonNull(lastIdxTick) ? totalOpenInterest - lastIdxTick.openInterest() : DoubleStream.of(priceMat.getColumn(12)).sum();
		
		// 合计成交量
		totalVolume = (long) DoubleStream.of(priceMat.getColumn(8)).sum();
		totalVolumeDelta = Objects.nonNull(lastIdxTick) ? totalVolume - lastIdxTick.volume() : (long) DoubleStream.of(priceMat.getColumn(9)).sum();
		// 合计成交额
		totalTurnover = (long) DoubleStream.of(priceMat.getColumn(10)).sum();
		totalTurnoverDelta = Objects.nonNull(lastIdxTick) ? totalTurnover - lastIdxTick.turnover() : (long) DoubleStream.of(priceMat.getColumn(11)).sum();
		
		openPrice = roundWithPriceTick(DoubleStream.of(bcResult.getColumn(1)).sum());
		highPrice = roundWithPriceTick(DoubleStream.of(bcResult.getColumn(2)).sum());
		lowPrice = roundWithPriceTick(DoubleStream.of(bcResult.getColumn(3)).sum());
		lastPrice = roundWithPriceTick(DoubleStream.of(bcResult.getColumn(4)).sum());
		settlePrice = roundWithPriceTick(DoubleStream.of(bcResult.getColumn(5)).sum());
		preClose = roundWithPriceTick(DoubleStream.of(bcResult.getColumn(6)).sum());
		preSettlePrice = roundWithPriceTick(DoubleStream.of(bcResult.getColumn(7)).sum());
		
	}
	
	private RealMatrix matrixOf(List<Tick> ticks) {
	    int numFeatures = 14; // Open Interest, Open, High, Low, Last, Settle, Pre-Close, Pre-Settle prices
	    int numTicks = ticks.size();

	    // 创建一个空的矩阵
	    RealMatrix tickMatrix = new Array2DRowRealMatrix(numTicks, numFeatures);

	    // 填充矩阵
	    for (int i = 0; i < numTicks; i++) {
	        tickMatrix.setRow(i, vectorize(ticks.get(i)));
	    }
	    return tickMatrix;
	}

	private double[] vectorize(Tick t) {
	    return new double[] {
	        t.openInterest(),			// 0
	        t.openPrice(),				
	        t.highPrice(),				// 2
	        t.lowPrice(),
	        t.lastPrice(),				// 4
	        t.settlePrice(),
	        t.preClosePrice(),			// 6
	        t.preSettlePrice(),			
	        t.volume(),					// 8
	        t.volumeDelta(),
	        t.turnover(),				// 10
	        t.turnoverDelta(),
	        t.openInterestDelta(),		// 12
	        t.preOpenInterest()
	    };
	}

	private RealMatrix oiWeighted(RealMatrix matrix) {
	    RealMatrix oi = matrix.getColumnMatrix(0); // 获取第一列
	    
	    double sumOI = DoubleStream.of(oi.getColumn(0)).sum();
	    for (int i = 0; i < oi.getRowDimension(); i++) {
	        oi.setEntry(i, 0, oi.getEntry(i, 0) / sumOI);
	    }
	    return oi;
	}

	//四舍五入处理
	private double roundWithPriceTick(double weightedPrice) {
		int enlargePrice = (int) (weightedPrice * 1000);
		int enlargePriceTick = (int) (idxContract.contract().priceTick() * 1000);
		int numOfTicks = enlargePrice / enlargePriceTick;
		int tickCarry = (enlargePrice % enlargePriceTick) < (enlargePriceTick / 2) ? 0 : 1;
		  
		return  idxContract.contract().priceTick() * (numOfTicks + tickCarry);
	}
	
}
