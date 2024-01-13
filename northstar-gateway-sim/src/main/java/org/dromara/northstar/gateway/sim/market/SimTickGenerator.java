package org.dromara.northstar.gateway.sim.market;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.gateway.Gateway;

/**
 * 该算法使用随机数、正弦函数、正态分布，生成正负概率及下一个TICK的差价，从而得到下一个TICK
 * 之所以使用正弦函数是因为正弦函数的值具有一定的连续性，符合行情的运行特征；
 * 之所以使用正态分布是模拟行情的极端情况
 * @author KevinHuangwl
 *
 */
public class SimTickGenerator {
	
	private Double lastPrice;
	
	private double openInterest = ThreadLocalRandom.current().nextDouble(10000, 30000);
	
	private long volume;
	
	private double high = Double.MIN_VALUE;
	
	private double low = Double.MAX_VALUE;
	
	private Random rand = new Random();
	
	private double seed = Math.random();
	
	private Contract contract;
	
	private final int amplifier;
	
	public SimTickGenerator(Contract contract) {
		this.contract = contract;
		this.lastPrice = (5000D + ThreadLocalRandom.current().nextDouble(-2000, 3000)) * contract.priceTick();
		this.amplifier = (int) (1 / contract.priceTick());
	}
	
	public String tickSymbol() {
		return contract.unifiedSymbol();
	}

	public Tick generateNextTick(LocalDateTime ldt, Gateway gateway) {
		double priceTick = contract.priceTick() == 0 ? 1 : contract.priceTick();
		seed += Math.random();
		int lastNumberOfTick = (int) (lastPrice * amplifier) / (int)(priceTick * amplifier);
		int deltaTick = generateDeltaTick(seed);
		double latestPrice = (lastNumberOfTick + deltaTick) * priceTick;
		double bidPrice = (lastNumberOfTick + deltaTick - 1) * priceTick;
		double askPrice = (lastNumberOfTick + deltaTick + 1) * priceTick;
		int deltaVol = generateDeltaVol(seed);
		int deltaInterest = generateDeltaOpenInterest();
		
		openInterest += deltaInterest;
		volume += deltaVol;
		return Tick.builder()
				.lastPrice(lastPrice)
				.preSettlePrice(lastPrice)
				.gatewayId(gateway.gatewayId())
				.channelType(ChannelType.SIM)
				.askPrice(List.of(askPrice))
				.bidPrice(List.of(bidPrice))
				.askVolume(List.of(0))
				.bidVolume(List.of(0))
				.contract(contract)
				.tradingDay(ldt.toLocalDate())
				.actionDay(ldt.toLocalDate())
				.actionTime(ldt.toLocalTime())
				.actionTimestamp(System.currentTimeMillis())
				.openInterest(openInterest)
				.openInterestDelta(deltaInterest)
				.volume(volume)
				.volumeDelta(deltaVol)
				.highPrice(high)
				.lowPrice(low)
				.type(TickType.MARKET_TICK)
				.lastPrice(latestPrice)
				.build();
	}
	
	private int generateDeltaTick(double seed) {
		double randSin = Math.sin(seed);
		double gaussianVal = rand.nextGaussian();
		int dirFactor = randSin == Math.abs(randSin) ? 1 : -1;	// 根据正弦值算方向
		dirFactor = Math.random() < Math.abs(randSin) ? dirFactor : -1 * dirFactor; // 根据概率复算方向
		return (int) (dirFactor * (Math.abs(gaussianVal) + 0.5));		//根据高斯值算变动TICK
	}
	
	private int generateDeltaVol(double seed) {
		return (int) (rand.nextInt(100) * Math.abs(Math.sin(seed)) * (Math.abs(rand.nextGaussian()) + 0.5)); 
	}
	
	private int generateDeltaOpenInterest() {
		return (int) (rand.nextInt(100) * rand.nextGaussian()); 
	}
	
}
