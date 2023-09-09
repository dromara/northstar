package org.dromara.northstar.gateway.sim.market;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.TickType;

import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 该算法使用随机数、正弦函数、正态分布，生成正负概率及下一个TICK的差价，从而得到下一个TICK
 * 之所以使用正弦函数是因为正弦函数的值具有一定的连续性，符合行情的运行特征；
 * 之所以使用正态分布是模拟行情的极端情况
 * @author KevinHuangwl
 *
 */
public class SimTickGenerator {
	
	private Double initPrice = 5000D + ThreadLocalRandom.current().nextDouble(-2000, 3000);
	
	private Double lastPrice = initPrice;
	
	private Random rand = new Random();
	
	private double seed = Math.random();
	
	private ContractField contract;
	
	public SimTickGenerator(ContractField contract) {
		this.contract = contract;
	}
	
	public ContractField contract() {
		return contract;
	}

	public TickField generateNextTick(LocalDateTime ldt) {
		double priceTick = contract.getPriceTick() == 0 ? 1 : contract.getPriceTick();
		seed += Math.random();
		TickField.Builder tb = TickField.newBuilder()
				.setLastPrice(lastPrice)
				.setPreSettlePrice(lastPrice)
				.setGatewayId(contract.getGatewayId())
				.setChannelType(ChannelType.SIM.toString())
				.addAllAskPrice(List.of(0D, 0D, 0D, 0D, 0D))
				.addAllBidPrice(List.of(0D, 0D, 0D, 0D, 0D))
				.addAllAskVolume(List.of(0, 0, 0, 0, 0))
				.addAllBidVolume(List.of(0, 0, 0, 0, 0))
				.setUnifiedSymbol(contract.getUnifiedSymbol())
				.setActionDay(ldt.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime(ldt.format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER))
				.setActionTimestamp(ldt.toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
		int lastNumberOfTick = (int) (lastPrice * 100) / (int)(priceTick * 100);
		int deltaTick = generateDeltaTick(seed);
		double latestPrice = (lastNumberOfTick + deltaTick) * priceTick;
		double bidPrice = (lastNumberOfTick + deltaTick - 1) * priceTick;
		double askPrice = (lastNumberOfTick + deltaTick + 1) * priceTick;
		int deltaVol = generateDeltaVol(seed);
		int deltaInterest = generateDeltaOpenInterest();
		
		String newActionDay = ldt.format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
		String oldActionDay = tb.getActionDay();
		double high = StringUtils.equals(newActionDay, oldActionDay) ? Math.max(tb.getHighPrice(), latestPrice) : Math.max(0, latestPrice);
		double low = StringUtils.equals(newActionDay, oldActionDay) ? Math.min(tb.getLowPrice(), latestPrice) : Math.min(Integer.MAX_VALUE, latestPrice);
		lastPrice = latestPrice;
		return tb.setActionDay(ldt.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setTradingDay(ldt.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime(ldt.format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER))
				.setActionTimestamp(ldt.toInstant(ZoneOffset.ofHours(8)).toEpochMilli())
				.setAskPrice(0, askPrice)
				.setBidPrice(0, bidPrice)
				.setOpenInterest(tb.getOpenInterest() + deltaInterest)
				.setOpenInterestDelta(deltaInterest)
				.setVolume(tb.getVolume() + deltaVol)
				.setVolumeDelta(deltaVol)
				.setHighPrice(high)
				.setLowPrice(low)
				.setStatus(TickType.NORMAL_TICK.getCode())
				.setLastPrice(latestPrice)
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
