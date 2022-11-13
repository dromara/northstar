package tech.quantit.northstar.gateway.playback.ticker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import xyz.redtorch.pb.CoreField.BarField;

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
	public List<TickEntry> generateFrom(BarField bar) {
		boolean up = bar.getClosePrice() > bar.getOpenPrice();
		List<Double> milestonePrices = List.of(
				bar.getOpenPrice(),
				up ? bar.getLowPrice() : bar.getHighPrice(),
				up ? bar.getHighPrice() : bar.getLowPrice(),
				bar.getClosePrice()
				);
		PriceRandomWalk pricer = new PriceRandomWalk(numOfTickPerBar, milestonePrices, priceTick);
		VolumeRandomWalk voler = new VolumeRandomWalk(numOfTickPerBar, 0);
		OpenInterestRandomWalk oier = new OpenInterestRandomWalk(numOfTickPerBar);
		List<Double> prices = pricer.generate();
		List<Long> volumes = voler.generate();
		List<Double> openInterests = oier.generate();
		List<TickEntry> ticks = new ArrayList<>(numOfTickPerBar);
		for(int i=0; i<numOfTickPerBar; i++) {
			ticks.add(TickEntry.of(prices.get(i), volumes.get(i), openInterests.get(i), bar.getActionTimestamp() - 60000 + i * 59990 / numOfTickPerBar));
		}
		return ticks;
	}
}

/**
 * 价格算法：
 * 初始价取K线开盘价，然后随机分配接下来的目标价，可能是[high, low, close]，也可能是[low, high, close];
 * 确定了价格运行路径后，就可以确定总步长（以最小变动价位为一个基础步长，例如最小变动价位为2，从开盘价5000至最高价5020，所需步长为10）；
 * 每个K线包含的TICK数视为总步数，可以得到平均步长（假设一分钟内总步长为20个价位变动，该分钟生成30个数据点，则平均步长为0.67）；
 * 把平均步长视为概率（即朝目标前进的概率，剩余的概率就是反向运动与原地停留的概率和），这样可以保留一定的随机运动；
 * 随着可用的数据点变小，往前运动的概率会自然增加；
 * 当概率大于1时，例如达到1.4，则代表步长变化至少为1个价位，在一个价位的基础上，40%的概率再加1，60%的概率为加0或-1；
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
		double std = stepSize * 2;
		int times = (int) Math.ceil(2 * std);	// 正负两倍标准差
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
		double stepSize = (double) walkingPath.size() / numOfTickPerBar;
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
			double avgStep = (double) (walkingPath.size() - cursor) / restStep;
			int randStep = (int) (ThreadLocalRandom.current().nextGaussian(avgStep, Math.max(1, stepSize)));
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
 * @author KevinHuangwl
 *
 */
class VolumeRandomWalk {
	
	int numOfTickPerBar;
	
	long volumeDelta;
	
	VolumeRandomWalk(int numOfTickPerBar, long volumeDelta){
		this.numOfTickPerBar = numOfTickPerBar;
		this.volumeDelta = volumeDelta;
	}
	
	List<Long> generate(){
		List<Long> results = new ArrayList<>(numOfTickPerBar);
		for(int i=0; i<numOfTickPerBar; i++) {
			results.add(0L);
		}
		return results;
	}
}

/**
 * 持仓量算法：
 * @author KevinHuangwl
 *
 */
class OpenInterestRandomWalk {
	
	int numOfTickPerBar;
	
	OpenInterestRandomWalk(int numOfTickPerBar){
		this.numOfTickPerBar = numOfTickPerBar;
	}
	
	List<Double> generate(){
		List<Double> results = new ArrayList<>(numOfTickPerBar);
		for(int i=0; i<numOfTickPerBar; i++) {
			results.add(0D);
		}
		return results;
	}
}