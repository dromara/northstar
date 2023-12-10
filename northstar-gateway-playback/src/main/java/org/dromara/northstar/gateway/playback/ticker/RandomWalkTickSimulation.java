package org.dromara.northstar.gateway.playback.ticker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import org.dromara.northstar.common.model.core.Bar;

/**
 * 随机漫步TICK仿真算法
 * 
 * @author KevinHuangwl
 *
 */
public class RandomWalkTickSimulation implements TickSimulationAlgorithm {
	
	private int numOfTickPerBar;
	
	private double priceTick;
	
	public RandomWalkTickSimulation(int numOfTickPerBar, double priceTick) {
		this.numOfTickPerBar = numOfTickPerBar;
		this.priceTick = priceTick;
	}
	
	@Override
	public List<TickEntry> generateFrom(Bar bar) {
		boolean up = bar.closePrice() > bar.openPrice();
		List<Double> milestonePrices = List.of(
				bar.openPrice(),
				up ? bar.lowPrice() : bar.highPrice(),
				up ? bar.highPrice() : bar.lowPrice(),
				bar.closePrice()
				);
		double rangeFactor = (bar.highPrice() - bar.lowPrice()) / bar.closePrice();
		double actualDiffRatio = Math.abs(bar.closePrice() - bar.openPrice()) / (bar.highPrice() - bar.lowPrice());
		PriceRandomWalk pricer = new PriceRandomWalk(numOfTickPerBar, milestonePrices, priceTick);
		VolumeRandomWalk voler = new VolumeRandomWalk(numOfTickPerBar, bar.volume());
		OpenInterestRandomWalk oier = new OpenInterestRandomWalk(numOfTickPerBar, bar.openInterestDelta(), rangeFactor, actualDiffRatio);
		List<Double> prices = pricer.generate();
		List<Long> volumes = voler.generate();
		List<Double> openInterests = oier.generate();
		List<TickEntry> ticks = new ArrayList<>(numOfTickPerBar);
		for(int i=0; i<numOfTickPerBar; i++) {
			double askPrice = prices.get(i);
			double bidPrice = prices.get(i);
			if(i == 0) {
				askPrice = prices.get(i) + ThreadLocalRandom.current().nextInt(2) * priceTick;
				bidPrice = askPrice - priceTick;
			} else if(prices.get(i) > ticks.get(i - 1).price()) {
				bidPrice = prices.get(i) - priceTick;	// 当价位上升时，代表最新价=卖一价，即买一价要小于最新价 
			} else {
				askPrice = prices.get(i) + priceTick;	// 当价位下跌时，代表最新价=买一价，即卖一价要大于最新价
			}
			ticks.add(TickEntry.of(prices.get(i), askPrice, bidPrice, volumes.get(i), openInterests.get(i), bar.actionTimestamp() - 60000 + i * 59990 / numOfTickPerBar));
		}
		return ticks;
	}
}

/**
 * 价格算法：
 * 初始价取K线开盘价，然后随机分配接下来的目标价，可能是[high, low, close]，也可能是[low, high, close];
 * 确定了价格运行路径后，就可以确定总步长（以最小变动价位为一个基础步长，例如最小变动价位为2，从开盘价5000至最高价5020，所需步长为10）；
 * 每个K线包含的TICK数视为总步数，可以得到平均步长（假设一分钟内总步长为20个价位变动，该分钟生成30个数据点，则平均步长为0.67）；同理可以得到剩余平均步长；
 * 把剩余平均步长视为高斯均值，然后求高斯随机值，这样可以保留一定的随机运动；
 * 随着可用的数据点变小，剩余平均步长会自然提高，往前运动的概率会自然增加；
 * @author KevinHuangwl
 *
 */
class PriceRandomWalk {
	
	List<Double> walkingPath;
	
	int cursor;
	
	double priceTick;
	
	int numOfTickPerBar;
	
	PriceRandomWalk(int numOfTickPerBar, List<Double> milestonePrices, double priceTick){
		this.numOfTickPerBar = numOfTickPerBar;
		this.priceTick = priceTick;
		this.walkingPath = getWalkingPath(milestonePrices);
	}
	
	private List<Double> getWalkingPath(List<Double> milestonePrices){
		List<Double> pathway = new ArrayList<>();
		Double curStep = null;
		for(Double milestonePrice : milestonePrices) {
			do {
				if(Objects.isNull(curStep)) {
					curStep = milestonePrice;
				} else if(milestonePrice > curStep) {
					curStep += priceTick;
				} else if(milestonePrice < curStep) {
					curStep -= priceTick;
				}
				pathway.add(curStep);
			} while(!appxEquals(milestonePrice, curStep));
		}
		double stepSize = (double) pathway.size() / numOfTickPerBar;
		double std = stepSize;
		int times = (int) (4 * std);	// 正负两倍标准差
		// 增加最高价的权重
		double highest = pathway.stream().mapToDouble(Double::doubleValue).max().getAsDouble();
		int indexOfHigh = pathway.indexOf(highest);
		for(int i=0; i<times; i++) {
			pathway.add(indexOfHigh, highest);
		}
		// 增加最低价的权重
		double lowest = pathway.stream().mapToDouble(Double::doubleValue).min().getAsDouble();
		int indexOfLow = pathway.indexOf(lowest);
		for(int i=0; i<times; i++) {
			pathway.add(indexOfLow, lowest);
		}
		return pathway;
	}
	
	private boolean appxEquals(double v1, double v2) {
		return Math.abs(v1 - v2) < priceTick / 2;
	}
	
	List<Double> generate(){
		List<Double> results = new ArrayList<>(numOfTickPerBar);
		for(int i=0; i<numOfTickPerBar; i++) {
			int restStep = numOfTickPerBar - i;
			if(i == 0) {
				results.add(walkingPath.get(0));
				continue;
			}
			if(restStep == 1) {
				results.add(walkingPath.get(walkingPath.size() - 1)); 	// 最后一步确保是收盘价
				break;
			}
			double avgStep = (double) (walkingPath.size() - cursor) / restStep; // 剩余平均步长
			int randStep = (int) (ThreadLocalRandom.current().nextGaussian(avgStep, Math.E)); // 使用高斯随机数保证波动的随机性
			cursor = safeAdd(cursor, randStep);
			results.add(walkingPath.get(cursor));
		}
		return results;
	}
	
	private int safeAdd(int cursor, int delta) {
		return Math.min(Math.max(0, cursor + delta), walkingPath.size() - 1);
	}
	
}

/**
 * 成交量算法：
 * 成交量的变化并不像价格变化那样有规律，只能随便模拟。
 * 
 * @author KevinHuangwl
 *
 */
class VolumeRandomWalk {
	
	int numOfTickPerBar;
	
	long volume;
	
	VolumeRandomWalk(int numOfTickPerBar, long volume){
		this.numOfTickPerBar = numOfTickPerBar;
		this.volume = volume;
	}
	
	List<Long> generate(){
		long sumVol = 0;
		List<Long> results = new ArrayList<>(numOfTickPerBar);
		for(int i=0; i<numOfTickPerBar; i++) {
			if(numOfTickPerBar - i == 1) {
				results.add(Math.max(volume - sumVol, 0));
				break;
			}
			double restAvgVol = (double) (volume - sumVol) / (numOfTickPerBar - i);
			long vol = (long) ThreadLocalRandom.current().nextGaussian(restAvgVol, Math.max(10, restAvgVol / 2));
			if(vol < 1) {
				vol = i + 2 < numOfTickPerBar ? (long) restAvgVol : 1;
			}
			results.add(vol);
			sumVol += vol;
		}
		return results;
	}
}

/**
 * 持仓量算法：
 * 持仓量的变化并不像价格变化那样有规律，只能随便模拟。
 * 有几种情况：
 * K线震幅与实际波幅比较大，即大阴大阳线，持仓量会倾向于朝一个方向变化，这时标准差会相对小；
 * K线震幅远大于实际波幅，例如十字星，持仓量变化也会反复不定，这时标准差会大；
 * K线震幅比较小，标准差会相对小。
 * 定义两个系数来描述以上情况：震幅系数与实际波幅比例系数
 * @author KevinHuangwl
 *
 */
class OpenInterestRandomWalk {
	
	int numOfTickPerBar;
	
	double openInterestDelta;
	
	double stdRef;
	
	OpenInterestRandomWalk(int numOfTickPerBar, double openInterestDelta, double rangeFactor, double actualDiffRatio){
		this.numOfTickPerBar = numOfTickPerBar;
		this.openInterestDelta = openInterestDelta;
		if(rangeFactor < 0.003) {	// 价格震幅小于千分之3时，属于震幅较小
			stdRef = 50;
		} else if (actualDiffRatio < 0.2) { // 震幅较大，且实际波幅小于震幅的20%时，属于十字星 
			stdRef = 100;
		} else {
			stdRef = Math.log(Math.abs(openInterestDelta));
		}
	}
	
	List<Double> generate(){
		double avgDelta = openInterestDelta / numOfTickPerBar;
		List<Double> results = new ArrayList<>(numOfTickPerBar);
		double sumDelta = 0;
		for(int i=0; i<numOfTickPerBar; i++) {
			if(numOfTickPerBar - i == 1) {
				results.add(openInterestDelta - sumDelta);
				break;
			}
			int delta = (int) ThreadLocalRandom.current().nextGaussian(avgDelta, Math.max(stdRef, 1));
			results.add((double) delta);
			sumDelta += delta;
		}
		return results;
	}
}