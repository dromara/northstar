package org.dromara.northstar.gateway.common.domain.mktdata;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import org.dromara.northstar.gateway.common.domain.contract.IndexContract;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public class IndexTicker {

	private IndexContract idxContract;
	
	private Consumer<TickField> onTickCallback;
	
	private static final long PARA_THRESHOLD = 100;
	
	private final Set<String> memberContracts;
	
	private ConcurrentHashMap<String, TickField> tickMap = new ConcurrentHashMap<>(20);
	private ConcurrentHashMap<String, Double> weightedMap = new ConcurrentHashMap<>(20);
	
	private long lastTickTimestamp = -1;
	
	protected final TickField.Builder tickBuilder = TickField.newBuilder();
	
	public IndexTicker(IndexContract idxContract, Consumer<TickField> onTickCallback) {
		this.idxContract = idxContract;
		this.memberContracts = idxContract.memberContracts().stream().map(c -> c.contractField().getUnifiedSymbol()).collect(Collectors.toSet());
		this.onTickCallback = onTickCallback;
		tickBuilder.setUnifiedSymbol(idxContract.contractField().getUnifiedSymbol());
		tickBuilder.setGatewayId(idxContract.contractField().getGatewayId());
		tickBuilder.addAllAskPrice(Arrays.asList(0D,0D,0D,0D,0D));
		tickBuilder.addAllBidPrice(Arrays.asList(0D,0D,0D,0D,0D));
		tickBuilder.addAllAskVolume(Arrays.asList(0,0,0,0,0));
		tickBuilder.addAllBidVolume(Arrays.asList(0,0,0,0,0));
	}

	public Set<String> dependencySymbols() {
		return memberContracts;
	}

	public synchronized void update(TickField tick) {
		if(!dependencySymbols().contains(tick.getUnifiedSymbol())) {
			log.warn("[{}]指数TICK生成器，无法处理 [{}] 的行情数据", idxContract.contractField().getUnifiedSymbol(), tick.getUnifiedSymbol());
			return;
		}
		// 如果有过期的TICK数据(例如不活跃的合约),则并入下个K线
		if (lastTickTimestamp < tick.getActionTimestamp()) {
			if(lastTickTimestamp > 0) {
				//进行运算
				calculate();
				TickField t = tickBuilder.build();
				onTickCallback.accept(t);
			}
			
			// 新一个指数Tick
			lastTickTimestamp = tick.getActionTimestamp();
			tickBuilder.setTradingDay(tick.getTradingDay());
			tickBuilder.setActionDay(tick.getActionDay());
			tickBuilder.setActionTime(tick.getActionTime());
			tickBuilder.setActionTimestamp(tick.getActionTimestamp());
			tickBuilder.setPreClosePrice(tick.getPreClosePrice());
			tickBuilder.setPreOpenInterest(tick.getPreOpenInterest());
			tickBuilder.setPreSettlePrice(tick.getPreSettlePrice());
			tickBuilder.setStatus(tick.getStatus());
		}
		// 同一个指数Tick
		tickMap.compute(tick.getUnifiedSymbol(), (k, v) -> tick);
	}
	
	

	private void calculate() {
		// 合计持仓量
		final double totalOpenInterest = tickMap.reduceValuesToDouble(PARA_THRESHOLD, TickField::getOpenInterest, 0, (a, b) -> a + b);
		final double totalOpenInterestDelta = tickMap.reduceValuesToDouble(PARA_THRESHOLD, TickField::getOpenInterestDelta, 0, (a, b) -> a + b);
		// 合约权值计算
		tickMap.forEachEntry(PARA_THRESHOLD, e -> weightedMap.compute(e.getKey(), (k,v) -> e.getValue().getOpenInterest() * 1.0 / totalOpenInterest));
		
		// 合计成交量
		final long totalVolume = tickMap.reduceValuesToLong(PARA_THRESHOLD, TickField::getVolume, 0, (a, b) -> a + b);
		final long totalVolumeDelta = tickMap.reduceValuesToLong(PARA_THRESHOLD, TickField::getVolumeDelta, 0, (a, b) -> a + b);
		// 合计成交额
		final double totalTurnover = tickMap.reduceValuesToDouble(PARA_THRESHOLD, TickField::getTurnover, 0, (a, b) -> a + b);
		final double totalTurnoverDelta = tickMap.reduceValuesToDouble(PARA_THRESHOLD, TickField::getTurnoverDelta, 0, (a, b) -> a + b);
		
		//加权均价
		double rawWeightedLastPrice = computeWeightedValue(e -> tickMap.get(e.getKey()).getLastPrice() * e.getValue());
		double weightedLastPrice = roundWithPriceTick(rawWeightedLastPrice);	//通过最小变动价位校准的加权均价
		tickBuilder.setLastPrice(weightedLastPrice);
		
		//加权最高价
		double rawWeightedHighPrice = computeWeightedValue(e -> tickMap.get(e.getKey()).getHighPrice() * e.getValue());
		double weightedHighPrice = roundWithPriceTick(rawWeightedHighPrice);	//通过最小变动价位校准的加权均价
		tickBuilder.setHighPrice(weightedHighPrice);
		
		//加权最低价
		double rawWeightedLowPrice = computeWeightedValue(e -> tickMap.get(e.getKey()).getLowPrice() * e.getValue());
		double weightedLowPrice = roundWithPriceTick(rawWeightedLowPrice);		//通过最小变动价位校准的加权均价
		tickBuilder.setLowPrice(weightedLowPrice);
		
		//加权开盘价
		double rawWeightedOpenPrice = computeWeightedValue(e -> tickMap.get(e.getKey()).getOpenPrice() * e.getValue());
		double weightedOpenPrice = roundWithPriceTick(rawWeightedOpenPrice);	//通过最小变动价位校准的加权均价
		tickBuilder.setOpenPrice(weightedOpenPrice);
		
		tickBuilder.setVolume(totalVolume);
		tickBuilder.setVolumeDelta(totalVolumeDelta);
		tickBuilder.setOpenInterestDelta(totalOpenInterestDelta);
		tickBuilder.setOpenInterest(totalOpenInterest);
		tickBuilder.setTurnover(totalTurnover);
		tickBuilder.setTurnoverDelta(totalTurnoverDelta);
	}
	
	private double computeWeightedValue(ToDoubleFunction<Entry<String, Double>> transformer) {
		return weightedMap.reduceEntriesToDouble(
				PARA_THRESHOLD,
				transformer,
				0D, 
				(a, b) -> a + b);
	}

	//四舍五入处理
	private double roundWithPriceTick(double weightedPrice) {
		int enlargePrice = (int) (weightedPrice * 1000);
		int enlargePriceTick = (int) (idxContract.contractField().getPriceTick() * 1000);
		int numOfTicks = enlargePrice / enlargePriceTick;
		int tickCarry = (enlargePrice % enlargePriceTick) < (enlargePriceTick / 2) ? 0 : 1;
		  
		return  enlargePriceTick * (numOfTicks + tickCarry) * 1.0 / 1000;
	}
}
