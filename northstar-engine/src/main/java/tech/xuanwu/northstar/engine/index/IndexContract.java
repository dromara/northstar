package tech.xuanwu.northstar.engine.index;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;

import tech.xuanwu.northstar.common.constant.Constants;
import tech.xuanwu.northstar.common.utils.ContractNameResolver;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

public class IndexContract {
	protected final ContractField self;
	protected final TickField.Builder tickBuilder = TickField.newBuilder();
	
	private TickEventHandler tickHandler;
	
	private final long PARA_THRESHOLD = 100;
	
	private ConcurrentHashMap<String, TickField> tickMap = new ConcurrentHashMap<>(20);
	private ConcurrentHashMap<String, Double> weightedMap = new ConcurrentHashMap<>(20);
	
	private volatile long lastTickTimestamp = -1;

	public IndexContract(Collection<ContractField> seriesContracts, TickEventHandler tickHandler) {
		if(seriesContracts.isEmpty()) {
			throw new IllegalStateException("合约个数为零");
		}
		this.tickHandler = tickHandler;
		ContractField proto = seriesContracts.iterator().next();
		
		String symbolName = proto.getSymbol().replaceAll("\\d+", "");
		Optional<String> chnNameOpt = Optional.ofNullable(ContractNameResolver.getCNSymbolName(symbolName));
		String symbol = symbolName + Constants.INDEX_SUFFIX; // 代码
		String name = chnNameOpt.orElse(symbolName) + "指数"; // 简称
		String fullName = name; // 全称
		String unifiedSymbol = String.format("%s@%s@%s", symbol, proto.getExchange(), proto.getProductClass()); // 统一ID，通常是<合约代码@交易所代码@产品类型>
		String contractId = unifiedSymbol + proto.getGatewayId();
		
		self = proto.toBuilder()
				.setSymbol(symbol)
				.setThirdPartyId(symbol)
				.setFullName(fullName)
				.setName(name)
				.setUnifiedSymbol(unifiedSymbol)
				.setContractId(contractId)
				.build();
		
		tickBuilder.setUnifiedSymbol(unifiedSymbol);
		tickBuilder.setGatewayId(proto.getGatewayId());
		tickBuilder.addAllAskPrice(Arrays.asList(0D,0D,0D,0D,0D));
		tickBuilder.addAllBidPrice(Arrays.asList(0D,0D,0D,0D,0D));
		tickBuilder.addAllAskVolume(Arrays.asList(0,0,0,0,0));
		tickBuilder.addAllBidVolume(Arrays.asList(0,0,0,0,0));
	}

	public ContractField getContract() {
		return self;
	}

	public synchronized void updateByTick(TickField tick) {
		if (lastTickTimestamp != tick.getActionTimestamp()) {
			if(lastTickTimestamp > 0) {
				//进行运算
				calculate();
				TickField t = tickBuilder.build();
				tickHandler.onTick(t);
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
		}
		// 同一个指数Tick
		tickMap.compute(tick.getUnifiedSymbol(), (k, v) -> tick);
	}

	private void calculate() {
		// 合计持仓量
		final double totalOpenInterest = tickMap.reduceValuesToDouble(PARA_THRESHOLD, t -> t.getOpenInterest(), 0, (a, b) -> a + b);
		final double totalOpenInterestDelta = tickMap.reduceValuesToDouble(PARA_THRESHOLD, t -> t.getOpenInterestDelta(), 0, (a, b) -> a + b);
		// 合约权值计算
		tickMap.forEachEntry(PARA_THRESHOLD, e -> weightedMap.compute(e.getKey(), (k,v) -> e.getValue().getOpenInterest() * 1.0 / totalOpenInterest));
		
		// 合计成交量
		final long totalVolume = tickMap.reduceValuesToLong(PARA_THRESHOLD, t -> t.getVolume(), 0, (a, b) -> a + b);
		final long totalVolumeDelta = tickMap.reduceValuesToLong(PARA_THRESHOLD, t -> t.getVolumeDelta(), 0, (a, b) -> a + b);
		// 合计成交额
		final double totalTurnover = tickMap.reduceValuesToDouble(PARA_THRESHOLD, t -> t.getTurnover(), 0, (a, b) -> a + b);
		final double totalTurnoverDelta = tickMap.reduceValuesToDouble(PARA_THRESHOLD, t -> t.getTurnoverDelta(), 0, (a, b) -> a + b);
		
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
		int enlargePriceTick = (int) (self.getPriceTick() * 1000);
		int numOfTicks = enlargePrice / enlargePriceTick;
		int tickCarry = (enlargePrice % enlargePriceTick) < (enlargePriceTick / 2) ? 0 : 1;
		  
		return  enlargePriceTick * (numOfTicks + tickCarry) * 1.0 / 1000;
	}

	public interface TickEventHandler {

		void onTick(TickField tick);
	}
	
}
