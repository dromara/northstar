package org.dromara.northstar.gateway.sim.market;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
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
	
	private Random rand = new Random();

	public TickField generateNextTick(InstrumentHolder ins) {
		double seed = ins.getSeed() + Math.random();
		ins.setSeed(seed);
		ContractField contract = ins.getContract();
		double priceTick = contract.getPriceTick() == 0 ? 1 : contract.getPriceTick();
		TickField.Builder tb = ins.getLastTick();
		double lastPrice = tb.getLastPrice();
		int lastNumberOfTick = (int) (lastPrice * 100) / (int)(priceTick * 100);
		int deltaTick = generateDeltaTick(seed);
		double latestPrice = (lastNumberOfTick + deltaTick) * priceTick;
		double bidPrice = (lastNumberOfTick + deltaTick - 1) * priceTick;
		double askPrice = (lastNumberOfTick + deltaTick + 1) * priceTick;
		int deltaVol = generateDeltaVol(seed);
		int deltaInterest = generateDeltaOpenInterest();
		
		long rawMillisec = System.currentTimeMillis();
		long adjustMillisec = rawMillisec - rawMillisec % 500;
		LocalDateTime ldt = LocalDateTime.from(Instant.ofEpochMilli(adjustMillisec).atZone(ZoneId.systemDefault()));
		String newActionDay = ldt.format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
		String oldActionDay = tb.getActionDay();
		double high = StringUtils.equals(newActionDay, oldActionDay) ? Math.max(tb.getHighPrice(), latestPrice) : Math.max(0, latestPrice);
		double low = StringUtils.equals(newActionDay, oldActionDay) ? Math.min(tb.getLowPrice(), latestPrice) : Math.min(Integer.MAX_VALUE, latestPrice);
		
		tb.setActionDay(ldt.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
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
			.setLastPrice(latestPrice);
		ins.setLastTick(tb);
		return tb.build();
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
