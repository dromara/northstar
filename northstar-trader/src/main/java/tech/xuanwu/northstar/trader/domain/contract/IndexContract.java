package tech.xuanwu.northstar.trader.domain.contract;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tech.xuanwu.northstar.constant.CommonConstant;
import tech.xuanwu.northstar.utils.ContractNameResolver;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;


/**
 * 指数合约领域模型，负责计算指数行情
 * @author kevinhuangwl
 *
 */
public class IndexContract {

	private ContractField self;

	private TickField.Builder tickBuilder = TickField.newBuilder();
	
	private double priceTick;

	/* 记录合约对应的数组下标 */
	private Map<String, Integer> symbolIndexMap;
	/* 记录合约的数组下标 */
	private Set<Integer> memSet;

	private int contractSize;

	private TickEventHandler tickHandler;

	private double[] prices;
	private long[] volumeDeltas;
	private long[] volumes;
	private double[] openInterestDeltas;
	private double[] openInterests;
	private volatile double totalOpenInterest;

	public IndexContract(List<ContractField> seriesContracts, TickEventHandler tickHandler) {
		contractSize = seriesContracts.size();
		if(contractSize == 0) {
			throw new IllegalStateException("合约个数为零");
		}
		this.tickHandler = tickHandler;
		ContractField proto = seriesContracts.get(0);
		
		String symbolName = proto.getSymbol().replaceAll("\\d+", "");
		String chnName = ContractNameResolver.getCNSymbolName(symbolName);
		String symbol = symbolName + CommonConstant.INDEX_SUFFIX; // 代码
		String name = chnName + "指数"; // 简称
		String fullName = name; // 全称
		String unifiedSymbol = symbol + CommonConstant.at + proto.getExchange() + CommonConstant.at + proto.getProductClass(); // 统一ID，通常是<合约代码@交易所代码@产品类型>
		String contractId = unifiedSymbol + proto.getGatewayId();
		
		self = proto.toBuilder()
				.setSymbol(symbol)
				.setThirdPartyId(symbol)
				.setFullName(fullName)
				.setName(name)
				.setUnifiedSymbol(unifiedSymbol)
				.setContractId(contractId)
				.build();
		
		priceTick = proto.getPriceTick();
		symbolIndexMap = new HashMap<>(contractSize);
		memSet = new HashSet<>(contractSize);
		prices = new double[contractSize];
		volumeDeltas = new long[contractSize];
		volumes = new long[contractSize];
		openInterestDeltas = new double[contractSize];
		openInterests = new double[contractSize];

		for (int i = 0; i < seriesContracts.size(); i++) {
			symbolIndexMap.put(seriesContracts.get(i).getUnifiedSymbol(), i);
		}

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
		Integer contractIndex = symbolIndexMap.get(tick.getUnifiedSymbol());
		if (memSet.contains(contractIndex)) {
			tickHandler.onTick(calculate());
			// 重置缓存
			memSet.clear();
			totalOpenInterest = 0;
		}

		memSet.add(contractIndex);
		totalOpenInterest += tick.getOpenInterest();
		double lastPrice = tick.getLastPrice();
		prices[contractIndex] = lastPrice;
		volumeDeltas[contractIndex] = tick.getVolumeDelta();
		volumes[contractIndex] = tick.getVolume();
		openInterestDeltas[contractIndex] = tick.getOpenInterestDelta();
		openInterests[contractIndex] = tick.getOpenInterest();
	}

	private TickField calculate() {
		// 加权均价
		double weightedPrice = 0;
		// 合计成交量变化
		long totalVolumeDeltaInTick = 0;
		// 合计成交量
		long totalVolume = 0;
		// 合计持仓量变化
		double totalOpenInterestDeltaInTick = 0;

		for (int i = 0; i < contractSize; i++) {
			weightedPrice += prices[i] * openInterests[i] / totalOpenInterest;
			totalVolumeDeltaInTick += volumeDeltas[i];
			totalVolume += volumes[i];
			totalOpenInterestDeltaInTick += openInterestDeltas[i];
		}
		
		weightedPrice = roundWithPriceTick(weightedPrice);
		
		tickBuilder.setLastPrice(weightedPrice);
		if (tickBuilder.getHighPrice()==0 || tickBuilder.getHighPrice() < weightedPrice) {
			tickBuilder.setHighPrice(weightedPrice);
		}
		if (tickBuilder.getLowPrice()==0 || tickBuilder.getLowPrice() > weightedPrice) {
			tickBuilder.setLowPrice(weightedPrice);
		}
		if(tickBuilder.getOpenPrice()==0) {
			tickBuilder.setOpenPrice(weightedPrice);
		}
		tickBuilder.setVolume(totalVolume);
		tickBuilder.setVolumeDelta(totalVolumeDeltaInTick);
		tickBuilder.setOpenInterestDelta(totalOpenInterestDeltaInTick);
		tickBuilder.setOpenInterest(totalOpenInterest);
		return tickBuilder.build();
	}

	//四舍五入处理
	private double roundWithPriceTick(double weightedPrice) {
		int enlargePrice = (int) (weightedPrice * 1000);
		int enlargePriceTick = (int) (priceTick * 1000);
		int numOfTicks = enlargePrice / enlargePriceTick;
		int tickCarry = (enlargePrice % enlargePriceTick) < (enlargePriceTick / 2) ? 0 : 1;
		  
		return  enlargePriceTick * (numOfTicks + tickCarry) * 1.0 / 1000;
	}

	public interface TickEventHandler {

		void onTick(TickField tick);
	}
}
