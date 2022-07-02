package tech.quantit.northstar.gateway.playback.ticker;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.common.constant.PlaybackPrecision;
import tech.quantit.northstar.common.constant.TickType;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 三角函数TICK仿真算法
 * 先确定开高低收对应的三角函数值，其中最高价为1，最低价为-1
 * @author KevinHuangwl
 *
 */
public class TrigonometricTickSimulation implements TickSimulationAlgorithm {
	
	private IContractManager contractMgr;
	
	private PlaybackPrecision precision;
	
	private int totalSize;
	
	private String gatewayId;
	
	public TrigonometricTickSimulation(String gatewayId, PlaybackPrecision precision, IContractManager contractMgr) {
		this.gatewayId = gatewayId;
		this.precision = precision;
		this.contractMgr = contractMgr;
		this.totalSize = switch (precision) {
		case LOW -> 4;
		case MEDIUM -> 30;	// 中精度一共30个TICK
		case HIGH -> 120;	// 高精度一共120个TICK
		default -> throw new IllegalArgumentException("Unexpected value: " + precision);
		};
	}
	
	@Override
	public List<TickField> generateFrom(BarField bar) {
		ContractField contract = contractMgr.getContract(bar.getUnifiedSymbol());
		double priceTick = contract.getPriceTick();
		boolean isUp = bar.getClosePrice() > bar.getOpenPrice();  //是否为阳线
		int numOfPriceTickFromHighToLow = (int) Math.round((bar.getHighPrice() - bar.getLowPrice()) / priceTick);	// 最高最低价之间一共有多少个最小变动价位
		int numOfPriceTickFromOpenToLow = (int) Math.round((bar.getOpenPrice() - bar.getLowPrice()) / priceTick);
		int numOfPriceTickFromCloseToLow = (int) Math.round((bar.getClosePrice() - bar.getLowPrice()) / priceTick);
		double valuePerPriceTick = 2.0 / numOfPriceTickFromHighToLow;
		double offset = isUp ? 0 : Math.PI * 2;
		double highArcSinVal = Math.PI / 2;
		double lowArcSinVal = - (Math.PI / 2) + offset; 
		double openArcSinValTemp = Math.asin(-1 + numOfPriceTickFromOpenToLow * valuePerPriceTick);
		double openArcSinVal = isUp ? lowArcSinVal - Math.abs(openArcSinValTemp - lowArcSinVal) : openArcSinValTemp;
		double closeArcSinValTemp = Math.asin(-1 + numOfPriceTickFromCloseToLow * valuePerPriceTick);
		double closeArcSinVal = isUp ? highArcSinVal + Math.abs(highArcSinVal - closeArcSinValTemp) : closeArcSinValTemp + offset;
		
		int[] sectionLens = new int[3];
		sectionLens[0] = isUp ? numOfPriceTickFromOpenToLow : numOfPriceTickFromHighToLow - numOfPriceTickFromOpenToLow;
		sectionLens[1] = numOfPriceTickFromHighToLow;
		sectionLens[2] = isUp ? numOfPriceTickFromHighToLow - numOfPriceTickFromCloseToLow : numOfPriceTickFromCloseToLow;
		List<Double> ohlc = insertVals(List.of(openArcSinVal, closeArcSinVal, highArcSinVal, lowArcSinVal), sectionLens) ;
		List<Double> prices = ohlc.stream()
				.mapToDouble(Double::doubleValue)
				.map(Math::sin)
				.map(val -> Math.round((val + 1) / valuePerPriceTick) * priceTick + bar.getLowPrice())
				.mapToObj(Double::valueOf)
				.toList();
		List<TickField> ticks = new ArrayList<>(totalSize);
		int timeFrame = 60000 / totalSize;
		long tickVolDelta = bar.getVolumeDelta() / totalSize;
		long tickOpenInterestDelta = (long) (bar.getOpenInterestDelta() / totalSize);
		long tickTurnoverDelta = (long) (bar.getTurnoverDelta() / totalSize);
		long tickNumTradesDelta = bar.getNumTradesDelta() / totalSize;
		
		for(int i=0; i<totalSize; i++) {
			ticks.add(TickField.newBuilder()
						.setUnifiedSymbol(bar.getUnifiedSymbol())
						.setPreClosePrice(bar.getPreClosePrice())
						.setPreOpenInterest(bar.getPreOpenInterest())
						.setPreSettlePrice(bar.getPreSettlePrice())
						.setTradingDay(bar.getTradingDay())
						.setLastPrice(prices.get(i))
						.setStatus(TickType.NORMAL_TICK.getCode())
						.setActionDay(bar.getActionDay())
						.setActionTime(bar.getActionTime())
						.setActionTimestamp(bar.getActionTimestamp() - (totalSize - i) * timeFrame)
						.addAllAskPrice(List.of(prices.get(i) + priceTick, 0D, 0D, 0D, 0D)) // 仅模拟卖一价
						.addAllBidPrice(List.of(prices.get(i) - priceTick, 0D, 0D, 0D, 0D)) // 仅模拟买一价
						.setGatewayId(gatewayId)
						.setHighPrice(bar.getHighPrice())	
						.setLowPrice(bar.getLowPrice())		
						.setLowerLimit(0)
						.setUpperLimit(Integer.MAX_VALUE)
						.setVolumeDelta(tickVolDelta)
						.setVolume(bar.getVolume() - (totalSize - 1 - i) * tickVolDelta)
						.setOpenInterestDelta(tickOpenInterestDelta)
						.setOpenInterest(bar.getOpenInterest() - (totalSize - 1 - i) * tickOpenInterestDelta)
						.setTurnoverDelta(tickTurnoverDelta)
						.setTurnover(bar.getTurnover() - (totalSize - 1 - i) * tickTurnoverDelta)
						.setNumTradesDelta(tickNumTradesDelta)
						.setNumTrades(bar.getNumTrades() - (totalSize - 1 - i) * tickNumTradesDelta)
						.build());
		}
		return ticks;
	}
	
	private List<Double> insertVals(List<Double> source, int[] sectionLen){
		double[] sourceArr = source.stream()
				.mapToDouble(Double::doubleValue)
				.sorted()
				.toArray();
		if(precision == PlaybackPrecision.LOW) {
			return DoubleStream.of(sourceArr)
					.mapToObj(Double::valueOf)
					.toList();
		}
		// 插值分为前中后三段
		int totalStep = IntStream.of(sectionLen).sum();
		int actualStep = totalSize - 4;
		double convertFactor = actualStep * 1.0 / totalStep;  
		sectionLen[0] *= convertFactor;
		sectionLen[2] *= convertFactor;
		sectionLen[1] = actualStep - sectionLen[0] - sectionLen[2];
		List<Double> resultList = new ArrayList<>(totalSize);
		for(int i=0; i<3; i++) {
			resultList.add(sourceArr[i]);
			resultList.addAll(makeSectionValues(sourceArr[i], sourceArr[i+1], sectionLen[i]));
		}
		resultList.add(sourceArr[3]);
		return resultList;
	}
	
	private Random rand = new Random();
	private List<Double> makeSectionValues(double rangeLow, double rangeHigh, int numOfValToInsert){
		List<Double> sectionValues = new ArrayList<>();
		for(int i=0; i<numOfValToInsert; i++) {
			sectionValues.add(rand.nextDouble(rangeLow, rangeHigh));
		}
		return sectionValues.stream().sorted().toList();
	}
	
}
